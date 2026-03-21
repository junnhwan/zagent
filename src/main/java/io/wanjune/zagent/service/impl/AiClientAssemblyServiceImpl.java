package io.wanjune.zagent.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.wanjune.zagent.advisor.RagContextAdvisor;
import io.wanjune.zagent.common.Constants;
import io.wanjune.zagent.mapper.*;
import io.wanjune.zagent.model.dto.McpRuntimeState;
import io.wanjune.zagent.model.entity.*;
import io.wanjune.zagent.model.enums.AdvisorTypeEnum;
import io.wanjune.zagent.model.enums.TransportTypeEnum;
import io.wanjune.zagent.service.AiClientAssemblyService;
import io.wanjune.zagent.service.McpBindingResolver;
import io.wanjune.zagent.service.McpConfigSyncService;
import io.wanjune.zagent.service.McpTransportConfigParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * AI鐎广垺鍩涚粩顖氬З閹浇顥婇柊宥嗘箛閸斺€崇杽閻滆埇鈧?
 * <p>閺嶇绺惧ù浣衡柤: clientId -> DB閺屻儴顕楅柊宥囩枂 -> 閺嬪嫬缂揙penAiApi -> 閺嬪嫬缂揅hatModel -> 鐟佸懘鍘dvisors/MCP瀹搞儱鍙?-> 鏉╂柨娲朇hatClient閵?
 * 娴ｈ法鏁?{@link ConcurrentHashMap} 缂傛挸鐡ㄥ鍙夌€铏规畱ChatClient鐎圭偘绶? 闁灝鍘ら柌宥咁槻閺嬪嫬缂撻妴?
 * 鐎圭偟骞?DisposableBean 閸︺劌绨查悽銊ュ彠闂傤厽妞傚〒鍛倞MCP鐎广垺鍩涚粩顖濈カ濠ф劑鈧?/p>
 */
@Slf4j
@Service
public class AiClientAssemblyServiceImpl implements AiClientAssemblyService, org.springframework.beans.factory.DisposableBean {

    private final ConcurrentHashMap<String, ChatClient> clientCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, McpRuntimeState> mcpRuntimeStates = new ConcurrentHashMap<>();
    /** MCP鐎广垺鍩涚粩顖濈カ濠ф劖鐫? 閻劋绨崗鎶芥４閺冭埖绔婚悶?*/
    private final List<McpSyncClient> mcpClientPool = Collections.synchronizedList(new ArrayList<>());

    @Resource
    private AiClientApiMapper aiClientApiMapper;
    @Resource
    private AiClientModelMapper aiClientModelMapper;
    @Resource
    private AiClientSystemPromptMapper aiClientSystemPromptMapper;
    @Resource
    private AiClientAdvisorMapper aiClientAdvisorMapper;
    @Resource
    private AiClientToolMcpMapper aiClientToolMcpMapper;
    @Resource
    private AiAgentFlowConfigMapper aiAgentFlowConfigMapper;
    @Resource
    @Qualifier("vectorStore")
    private VectorStore vectorStore;
    @Resource
    @Qualifier("executorService")
    private ThreadPoolExecutor executorService;
    @Resource
    private McpConfigSyncService mcpConfigSyncService;
    @Resource
    private McpBindingResolver mcpBindingResolver;
    @Resource
    private McpTransportConfigParser mcpTransportConfigParser;

    /** {@inheritDoc} */
    @Override
    public ChatClient getOrBuildChatClient(String clientId) {
        return clientCache.computeIfAbsent(clientId, this::buildChatClient);
    }

    /** {@inheritDoc} */
    @Override
    public void invalidate(String clientId) {
        clientCache.remove(clientId);
        log.info("ChatClient cache invalidated for clientId: {}", clientId);
    }

    /**
     * 鎼存梻鏁ら崗鎶芥４閺冭埖绔婚悶鍡樺閺堝CP鐎广垺鍩涚粩顖濈カ濠?
     */
    @Override
    public void destroy() {
        log.info("Closing {} MCP clients before shutdown", mcpClientPool.size());
        for (McpSyncClient client : mcpClientPool) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Failed to close MCP client cleanly: {}", e.getMessage());
            }
        }
        mcpClientPool.clear();
        clientCache.clear();
        log.info("MCP clients and ChatClient cache cleared");
    }

    /**
     * 閸氼垰濮╂０鍕劰: 瀵倹顒炴０鍕€鐑樺閺堝lowConfig娑擃厼绱╅悽銊ф畱ChatClient
     */
    @PostConstruct
    public void init() {
        executorService.execute(() -> {
            try {
                mcpConfigSyncService.syncIfEnabled();
                Thread.sleep(3000); // 缁涘绶熼崗鏈电铂Bean閸掓繂顫愰崠鏍х暚閹?
                warmUpAll();
            } catch (Exception e) {
                log.warn("Failed to load MCP sync config during startup warm-up: {}", e.getMessage());
            }
        });
    }

    @Override
    public void warmUpAll() {
        log.info("Starting ChatClient warm-up...");
        try {
            // 閼惧嘲褰囬幍鈧張濉卨owConfig娑擃厼绱╅悽銊ф畱clientId閿涘牆骞撻柌宥忕礆
            List<AiAgentFlowConfig> allFlowConfigs = aiAgentFlowConfigMapper.selectAll();
            Set<String> clientIds = new LinkedHashSet<>();
            for (AiAgentFlowConfig config : allFlowConfigs) {
                clientIds.add(config.getClientId());
            }

            int success = 0;
            for (String clientId : clientIds) {
                try {
                    getOrBuildChatClient(clientId);
                    success++;
                } catch (Exception e) {
                    log.warn("Warm-up failed for clientId={}: {}", clientId, e.getMessage());
                }
            }
            log.info("Warm-up finished: {}/{} ChatClients built successfully", success, clientIds.size());
        } catch (Exception e) {
            log.warn("ChatClient warm-up aborted: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, McpRuntimeState> getMcpRuntimeStates() {
        return Collections.unmodifiableMap(mcpRuntimeStates);
    }

    /**
     * 閺嶇绺鹃弬瑙勭《 - 娴犲孩鏆熼幑顔肩氨闁板秶鐤嗛弸鍕紦鐎瑰本鏆ｉ惃鍑渉atClient鐎圭偘绶ラ妴?
     * <p>閹笛嗩攽濮濄儵顎?
     * <ol>
     *   <li>閸旂姾娴嘽lientId鐎电懓绨查惃鍕閺堝鍘ょ純顔煎彠閼辨柨鍙х化?/li>
     *   <li>閹稿娲伴弽鍥╄閸ㄥ鍨庣紒? 楠炴儼顢戦崝鐘烘祰Model/Prompt/Advisor/MCP闁板秶鐤?/li>
     *   <li>閺嬪嫬缂揙penAiApi閿涘湏PI閸︽澘娼冮崪灞界槕闁姐儻绱?/li>
     *   <li>閺嬪嫬缂揗CP瀹搞儱鍙块崶鐐剁殶閸掓銆?/li>
     *   <li>閺嬪嫬缂揅hatModel閿涘牆鎯堝Ο鈥崇€烽崣鍌涙殶閸滃苯浼愰崗宄版礀鐠嬪喛绱?/li>
     *   <li>閹峰吋甯寸化鑽ょ埠閹绘劗銇氱拠?/li>
     *   <li>閺嬪嫬缂揂dvisor闁?/li>
     *   <li>缂佸嫯顥婇獮鎯扮箲閸ョ偞娓剁紒鍫㈡畱ChatClient</li>
     * </ol></p>
     *
     * @param clientId 鐎广垺鍩涚粩顖涚垼鐠囧捄D
     * @return 缂佸嫯顥婄€瑰本鍨氶惃鍑渉atClient鐎圭偘绶?
     */
    private ChatClient buildChatClient(String clientId) {
        log.info("Building ChatClient for clientId: {}", clientId);

        var bindingResolution = mcpBindingResolver.resolve(clientId);

        // 3. Parallel load all configuration data
        CompletableFuture<AiClientModel> modelFuture = CompletableFuture.supplyAsync(() -> {
            return aiClientModelMapper.selectByModelId(bindingResolution.modelId());
        }, executorService);

        CompletableFuture<List<AiClientSystemPrompt>> promptFuture = CompletableFuture.supplyAsync(() -> {
            List<String> promptIds = bindingResolution.promptIds();
            return promptIds.isEmpty() ? Collections.emptyList() : aiClientSystemPromptMapper.selectByPromptIds(promptIds);
        }, executorService);

        CompletableFuture<List<AiClientAdvisor>> advisorFuture = CompletableFuture.supplyAsync(() -> {
            List<String> advisorIds = bindingResolution.advisorIds();
            return advisorIds.isEmpty() ? Collections.emptyList() : aiClientAdvisorMapper.selectByAdvisorIds(advisorIds);
        }, executorService);

        // Also load MCP tools linked to the model
        CompletableFuture<List<AiClientToolMcp>> mcpFuture = CompletableFuture.supplyAsync(() -> {
            List<String> mcpIds = bindingResolution.mcpIds();
            return mcpIds.isEmpty() ? Collections.emptyList() : aiClientToolMcpMapper.selectByMcpIds(mcpIds);
        }, executorService);

        // Wait for all futures
        AiClientModel modelConfig = modelFuture.join();
        List<AiClientSystemPrompt> prompts = promptFuture.join();
        List<AiClientAdvisor> advisors = advisorFuture.join();
        List<AiClientToolMcp> mcpTools = mcpFuture.join();

        if (modelConfig == null) {
            throw new RuntimeException("No model config found for clientId: " + clientId);
        }

        // 4. Load API config for the model
        AiClientApi apiConfig = aiClientApiMapper.selectByApiId(modelConfig.getApiId());
        if (apiConfig == null) {
            throw new RuntimeException("No API config found for apiId: " + modelConfig.getApiId());
        }

        // 5. Build OpenAiApi
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(apiConfig.getBaseUrl())
                .apiKey(apiConfig.getApiKey())
                .completionsPath(apiConfig.getCompletionsPath())
                .embeddingsPath(apiConfig.getEmbeddingsPath())
                .build();

        // 6. Build MCP tool callbacks
        List<ToolCallback> toolCallbacks = buildMcpToolCallbacks(mcpTools);

        // 7. Build ChatModel
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(modelConfig.getModelName());
        if (!toolCallbacks.isEmpty()) {
            optionsBuilder.toolCallbacks(toolCallbacks);
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();

        // 8. Build system prompt
        String systemPrompt = prompts.stream()
                .map(AiClientSystemPrompt::getPromptContent)
                .collect(Collectors.joining("\n\n"));

        // 9. Build advisors
        List<Advisor> advisorList = buildAdvisors(advisors);

        // 10. Build ChatClient
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (!systemPrompt.isBlank()) {
            builder.defaultSystem(systemPrompt);
        }
        if (!advisorList.isEmpty()) {
            builder.defaultAdvisors(advisorList.toArray(new Advisor[0]));
        }
        if (!toolCallbacks.isEmpty()) {
            builder.defaultToolCallbacks(toolCallbacks);
        }

        log.info("ChatClient built successfully for clientId: {}, model: {}, advisors: {}, mcpTools: {}",
                clientId, modelConfig.getModelName(), advisors.size(), mcpTools.size());

        return builder.build();
    }

    /**
     * 閺嶈宓丮CP闁板秶鐤嗛崚娑樼紦瀹搞儱鍙块崶鐐剁殶閸掓銆冮妴?
     * <p>闁秴宸籑CP瀹搞儱鍙块柊宥囩枂, 娑撶儤鐦℃稉顏堝帳缂冾喖鍨卞绡梒pSyncClient, 閻掕泛鎮楅柅姘崇箖
     * {@link SyncMcpToolCallbackProvider} 缂佺喍绔存潪顒佸床娑撶oolCallback閸掓銆冮妴?/p>
     *
     * @param mcpTools MCP瀹搞儱鍙块柊宥囩枂閸掓銆?
     * @return 瀹搞儱鍙块崶鐐剁殶閸掓銆? 婵″倹鐏夐弮鐕P闁板秶鐤嗛崚娆掔箲閸ョ偟鈹栭崚妤勩€?
     */
    private List<ToolCallback> buildMcpToolCallbacks(List<AiClientToolMcp> mcpTools) {
        if (mcpTools == null || mcpTools.isEmpty()) {
            return Collections.emptyList();
        }

        List<McpSyncClient> mcpClients = new ArrayList<>();
        for (AiClientToolMcp mcp : mcpTools) {
            try {
                McpSyncClient mcpClient = createMcpClient(mcp);
                mcpClients.add(mcpClient);
                mcpClientPool.add(mcpClient); // 閸旂姴鍙嗙挧鍕爱濮? 閸忔娊妫撮弮鍓佺埠娑撯偓濞撳懐鎮?
                mcpRuntimeStates.put(mcp.getMcpId(), new McpRuntimeState(true, null, Instant.now()));
                log.info("MCP client initialized: {} ({})", mcp.getMcpName(), mcp.getTransportType());
            } catch (Exception e) {
                mcpRuntimeStates.put(mcp.getMcpId(), new McpRuntimeState(false, e.getMessage(), Instant.now()));
                log.error("Failed to create MCP client: {} - {}", mcp.getMcpName(), e.getMessage());
            }
        }

        if (mcpClients.isEmpty()) {
            return Collections.emptyList();
        }

        ToolCallback[] callbacks = new SyncMcpToolCallbackProvider(mcpClients).getToolCallbacks();
        return Arrays.asList(callbacks);
    }

    /**
     * 閺嶈宓佹导鐘虹翻缁鐎烽崚娑樼紦MCP鐎广垺鍩涚粩顖樷偓?
     * <p>閺€顖涘瘮SSE閸滃tdio娑撱倗顫掓导鐘虹翻閺傜懓绱? 閺嶈宓侀柊宥囩枂娑擃厾娈憈ransportType鐎涙顔岄懛顏勫З闁瀚ㄩ妴?/p>
     *
     * @param mcp MCP瀹搞儱鍙块柊宥囩枂鐎圭偘缍?
     * @return 閸掓繂顫愰崠鏍х暚閹存劗娈慚CP閸氬本顒炵€广垺鍩涚粩?
     */
    private McpSyncClient createMcpClient(AiClientToolMcp mcp) {
        TransportTypeEnum transportType = TransportTypeEnum.of(mcp.getTransportType());
        Duration timeout = Duration.ofMinutes(mcp.getRequestTimeout() != null ? mcp.getRequestTimeout() : 180);

        if (transportType == TransportTypeEnum.SSE) {
            return createSseMcpClient(mcp.getTransportConfig(), timeout);
        } else {
            return createStdioMcpClient(mcp.getTransportConfig(), timeout);
        }
    }

    /**
     * 閸掓稑缂揝SE娴肩姾绶惃鍑狢P鐎广垺鍩涚粩顖樷偓?
     * <p>娴犲窐ransportConfig JSON娑擃叀袙閺嬫亣aseUri閸滃seEndpoint, 閺嬪嫬缂揌ttpClientSseClientTransport閵?/p>
     *
     * @param transportConfig 娴肩姾绶柊宥囩枂JSON鐎涙顑佹稉? 閸栧懎鎯坆aseUri閸滃苯褰查柅澶屾畱sseEndpoint
     * @param timeout         鐠囬攱鐪扮搾鍛閺冨爼妫?
     * @return 閸掓繂顫愰崠鏍х暚閹存劗娈慚CP閸氬本顒炵€广垺鍩涚粩?
     */
    private McpSyncClient createSseMcpClient(String transportConfig, Duration timeout) {
        var config = mcpTransportConfigParser.parseSse(transportConfig);
        String baseUri = config.baseUri();
        String sseEndpoint = config.sseEndpoint();

        HttpClientSseClientTransport transport;
        if (sseEndpoint != null && !sseEndpoint.isEmpty()) {
            transport = HttpClientSseClientTransport.builder(baseUri + sseEndpoint).build();
        } else {
            transport = HttpClientSseClientTransport.builder(baseUri).build();
        }

        McpSyncClient client = McpClient.sync(transport).requestTimeout(timeout).build();
        client.initialize();
        return client;
    }

    /**
     * 閸掓稑缂揝tdio娴肩姾绶惃鍑狢P鐎广垺鍩涚粩顖樷偓?
     * <p>娴犲窐ransportConfig JSON娑擃叀袙閺嬫亪ommand閸滃畮rgs, 閺嬪嫬缂揝tdioClientTransport閵?
     * 闁板秶鐤嗛弽鐓庣础: {"toolName": {"command": "npx", "args": [...]}}</p>
     *
     * @param transportConfig 娴肩姾绶柊宥囩枂JSON鐎涙顑佹稉? 閸栧懎鎯堥崨鎴掓姢閸滃苯寮弫?
     * @param timeout         鐠囬攱鐪扮搾鍛閺冨爼妫?
     * @return 閸掓繂顫愰崠鏍х暚閹存劗娈慚CP閸氬本顒炵€广垺鍩涚粩?
     */
    private McpSyncClient createStdioMcpClient(String transportConfig, Duration timeout) {
        ServerParameters params = mcpTransportConfigParser.toServerParameters(
                mcpTransportConfigParser.parseStdio(transportConfig));

        McpSyncClient client = McpClient.sync(new StdioClientTransport(params))
                .requestTimeout(timeout).build();
        client.initialize();
        return client;
    }

    static ServerParameters buildServerParameters(String transportConfig) {
        McpTransportConfigParserImpl parser = new McpTransportConfigParserImpl();
        return parser.toServerParameters(parser.parseStdio(transportConfig));
    }

    static String normalizeSseBaseUri(String baseUri) {
        return McpTransportConfigParserImpl.normalizeSseBaseUri(baseUri);
    }

    static String normalizeSseEndpoint(String sseEndpoint) {
        return McpTransportConfigParserImpl.normalizeSseEndpoint(sseEndpoint);
    }

    /**
     * 閺嶈宓侀柊宥囩枂閺嬪嫬缂揂dvisor闁炬拝绱機hatMemory/RAG/Logger閿涘鈧?
     * <p>闁秴宸籄dvisor闁板秶鐤嗛崚妤勩€? 娑撶儤鐦℃稉顏堝帳缂冾喖鍨卞鍝勵嚠鎼存梻琚崹瀣畱Advisor鐎圭偘绶?
     * 楠炶泛婀張顐㈢啲婵绮撴潻钘夊 {@link SimpleLoggerAdvisor} 閻劋绨弮銉ョ箶鐠佹澘缍嶉妴?/p>
     *
     * @param advisorConfigs Advisor闁板秶鐤嗛崚妤勩€?
     * @return 閺嬪嫬缂撶€瑰本鍨氶惃鍑檇visor閸掓銆?
     */
    private List<Advisor> buildAdvisors(List<AiClientAdvisor> advisorConfigs) {
        List<Advisor> advisors = new ArrayList<>();
        for (AiClientAdvisor config : advisorConfigs) {
            try {
                Advisor advisor = createAdvisor(config);
                if (advisor != null) {
                    advisors.add(advisor);
                }
            } catch (Exception e) {
                log.error("Failed to create advisor: {} - {}", config.getAdvisorName(), e.getMessage());
            }
        }
        // Always add SimpleLoggerAdvisor
        advisors.add(SimpleLoggerAdvisor.builder().build());
        return advisors;
    }

    /**
     * 閺嶈宓侀柊宥囩枂閸掓稑缂撻崡鏇氶嚋Advisor鐎圭偘绶ラ妴?
     * <p>閺€顖涘瘮閻ㄥ嚈dvisor缁鐎?
     * <ul>
     *   <li>{@code CHAT_MEMORY} - 鐎电鐦界拋鏉跨箓Advisor, 閸╄桨绨☉鍫熶紖缁愭褰涙穱婵囧瘮娑撳﹣绗呴弬?/li>
     *   <li>{@code RAG_ANSWER} - RAG濡偓缁便垹顤冨绡坉visor, 娴犲骸鎮滈柌蹇撶氨濡偓缁便垻娴夐崗铏瀮濡楋絾鏁為崗銉ょ瑐娑撳鏋?/li>
     * </ul></p>
     *
     * @param config Advisor闁板秶鐤嗙€圭偘缍? 閸栧懎鎯堢猾璇茬€烽崪灞惧⒖鐏炴洖寮弫?
     * @return 閸掓稑缂撻惃鍑檇visor鐎圭偘绶?
     */
    private Advisor createAdvisor(AiClientAdvisor config) {
        AdvisorTypeEnum type = AdvisorTypeEnum.of(config.getAdvisorType());
        JSONObject extParam = config.getExtParam() != null ? JSON.parseObject(config.getExtParam()) : new JSONObject();

        return switch (type) {
            case CHAT_MEMORY -> {
                int maxMessages = extParam.getIntValue("maxMessages");
                if (maxMessages <= 0) maxMessages = 100;
                yield PromptChatMemoryAdvisor.builder(
                        MessageWindowChatMemory.builder().maxMessages(maxMessages).build()
                ).build();
            }
            case RAG_ANSWER -> {
                int topK = extParam.getIntValue("topK");
                if (topK <= 0) topK = 5;
                String filterExpression = extParam.getString("filterExpression");
                SearchRequest.Builder searchBuilder = SearchRequest.builder().topK(topK);
                if (filterExpression != null && !filterExpression.isBlank()) {
                    searchBuilder.filterExpression(filterExpression);
                }
                yield new RagContextAdvisor(vectorStore, searchBuilder.build());
            }
        };
    }

}
