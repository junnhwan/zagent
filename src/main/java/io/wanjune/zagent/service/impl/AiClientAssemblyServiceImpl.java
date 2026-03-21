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
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
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
 * AI Client 运行时装配服务实现。
 * <p>核心链路：clientId -> 解析数据库绑定关系 -> 构建 OpenAiApi -> 构建 ChatModel ->
 * 组装 Advisors / MCP Tools -> 生成 ChatClient。
 * 使用 {@link ConcurrentHashMap} 缓存已构建的 ChatClient，并在销毁时统一关闭 MCP 客户端。</p>
 */
@Slf4j
@Service
public class AiClientAssemblyServiceImpl implements AiClientAssemblyService, org.springframework.beans.factory.DisposableBean {

    private final ConcurrentHashMap<String, ChatClient> clientCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, McpRuntimeState> mcpRuntimeStates = new ConcurrentHashMap<>();
    /** 已初始化的 MCP 同步客户端池，用于应用关闭时统一释放资源。 */
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
     * 关闭缓存中的 MCP 客户端，并清空 ChatClient 缓存。
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
     * 启动后先同步 MCP 配置，再预热 Flow 中涉及到的 ChatClient。
     */
    @PostConstruct
    public void init() {
        executorService.execute(() -> {
            try {
                mcpConfigSyncService.syncIfEnabled();
                Thread.sleep(3000); // 等待相关 Bean 和外部资源完成初始化
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
            // 从所有 Flow 配置中提取需要预热的 clientId，避免重复构建
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
     * 按 clientId 动态构建 ChatClient。
     * <p>主要步骤：
     * <ol>
     *   <li>解析 clientId 对应的模型、Prompt、Advisor 和 MCP 绑定关系；</li>
     *   <li>并行加载 Model、Prompt、Advisor、MCP 配置；</li>
     *   <li>根据模型关联的 API 配置构建 {@link OpenAiApi}；</li>
     *   <li>将 MCP 配置转换为可调用的 ToolCallback；</li>
     *   <li>构建 {@link OpenAiChatModel} 并附加工具能力；</li>
     *   <li>拼接系统提示词；</li>
     *   <li>创建 Advisors；</li>
     *   <li>最终组装并返回 ChatClient。</li>
     * </ol></p>
     *
     * @param clientId 客户端 ID
     * @return 组装完成的 ChatClient
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
     * 将 MCP 配置转换为 Spring AI 可调用的工具回调。
     * <p>方法会先为每个 MCP 配置创建对应的 {@link McpSyncClient}，
     * 再通过 {@link SyncMcpToolCallbackProvider} 统一转换为 {@link ToolCallback}。</p>
     *
     * @param mcpTools MCP 工具配置列表
     * @return 可挂载到 ChatModel / ChatClient 的工具回调列表
     */
    private List<ToolCallback> buildMcpToolCallbacks(List<AiClientToolMcp> mcpTools) {
        if (mcpTools == null || mcpTools.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> bindingLabels = mcpTools.stream()
                .map(AiClientAssemblyServiceImpl::formatMcpBindingLabel)
                .toList();

        List<McpSyncClient> mcpClients = new ArrayList<>();
        for (AiClientToolMcp mcp : mcpTools) {
            try {
                McpSyncClient mcpClient = createMcpClient(mcp);
                mcpClients.add(mcpClient);
                mcpClientPool.add(mcpClient); // 纳入连接池，便于应用关闭时统一释放
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
        List<String> toolNames = Arrays.stream(callbacks)
                .map(callback -> safeToolName(callback.getToolDefinition()))
                .toList();

        log.info("MCP tool callbacks ready: bindings={}, tools={}", bindingLabels, toolNames);

        return Arrays.stream(callbacks)
                .map(callback -> wrapToolCallback(callback, bindingLabels))
                .toList();
    }

    private ToolCallback wrapToolCallback(ToolCallback delegate, List<String> bindingLabels) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public org.springframework.ai.tool.metadata.ToolMetadata getToolMetadata() {
                return delegate.getToolMetadata();
            }

            @Override
            public String call(String input) {
                String toolName = safeToolName(delegate.getToolDefinition());
                log.info("MCP tool invocation start: tool={}, bindings={}, inputPreview={}",
                        toolName, bindingLabels, abbreviateForLog(input));
                try {
                    String result = delegate.call(input);
                    log.info("MCP tool invocation success: tool={}, bindings={}, resultPreview={}",
                            toolName, bindingLabels, abbreviateForLog(result));
                    return result;
                } catch (Exception e) {
                    log.error("MCP tool invocation failed: tool={}, bindings={}, error={}",
                            toolName, bindingLabels, e.getMessage());
                    throw e;
                }
            }

            @Override
            public String call(String input, ToolContext toolContext) {
                String toolName = safeToolName(delegate.getToolDefinition());
                log.info("MCP tool invocation start: tool={}, bindings={}, inputPreview={}",
                        toolName, bindingLabels, abbreviateForLog(input));
                try {
                    String result = delegate.call(input, toolContext);
                    log.info("MCP tool invocation success: tool={}, bindings={}, resultPreview={}",
                            toolName, bindingLabels, abbreviateForLog(result));
                    return result;
                } catch (Exception e) {
                    log.error("MCP tool invocation failed: tool={}, bindings={}, error={}",
                            toolName, bindingLabels, e.getMessage());
                    throw e;
                }
            }
        };
    }

    static String formatMcpBindingLabel(AiClientToolMcp mcp) {
        return mcp.getMcpName() + "[" + mcp.getTransportType() + "]";
    }

    static String abbreviateForLog(String text) {
        if (text == null) {
            return "null";
        }
        String normalized = text.replaceAll("\s+", " ").trim();
        if (normalized.length() <= 160) {
            return normalized;
        }
        return normalized.substring(0, 157) + "...";
    }

    private static String safeToolName(ToolDefinition definition) {
        if (definition == null || definition.name() == null || definition.name().isBlank()) {
            return "unknown";
        }
        return definition.name();
    }

    /**
     * 按传输类型创建 MCP 同步客户端。
     * <p>当前支持 SSE 和 stdio 两种传输方式，具体创建逻辑由 transportType 决定。</p>
     *
     * @param mcp MCP 工具配置
     * @return 已初始化的 MCP 同步客户端
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
     * 创建基于 SSE 传输的 MCP 同步客户端。
     * <p>从 transportConfig JSON 中解析 baseUri 与 sseEndpoint，
     * 再构建 {@link HttpClientSseClientTransport}。</p>
     *
     * @param transportConfig SSE 传输配置 JSON
     * @param timeout 请求超时时间
     * @return 已初始化的 MCP 同步客户端
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
     * 创建基于 stdio 传输的 MCP 同步客户端。
     * <p>从 transportConfig JSON 中解析 command 与 args，
     * 再构建 {@link StdioClientTransport}。
     * 配置示例：{"toolName": {"command": "npx", "args": [...]}}</p>
     *
     * @param transportConfig stdio 传输配置 JSON
     * @param timeout 请求超时时间
     * @return 已初始化的 MCP 同步客户端
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
     * 根据配置创建 Advisor 列表，并追加默认日志 Advisor。
     * <p>当前支持 ChatMemory 与 RAG Advisor；无论是否配置，都会附加
     * {@link SimpleLoggerAdvisor} 以便输出调用日志。</p>
     *
     * @param advisorConfigs Advisor 配置列表
     * @return 已创建的 Advisor 列表
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
     * 根据单条配置创建具体的 Advisor 实例。
     * <p>当前支持：
     * <ul>
     *   <li>{@code CHAT_MEMORY}：基于消息窗口的会话记忆 Advisor；</li>
     *   <li>{@code RAG_ANSWER}：基于向量检索的 RAG 上下文增强 Advisor。</li>
     * </ul></p>
     *
     * @param config Advisor 配置
     * @return 创建好的 Advisor 实例
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
