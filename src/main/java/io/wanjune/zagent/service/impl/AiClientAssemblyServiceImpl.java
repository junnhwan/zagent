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
import io.wanjune.zagent.model.entity.*;
import io.wanjune.zagent.model.enums.AdvisorTypeEnum;
import io.wanjune.zagent.model.enums.TransportTypeEnum;
import io.wanjune.zagent.service.AiClientAssemblyService;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * AI客户端动态装配服务实现。
 * <p>核心流程: clientId -> DB查询配置 -> 构建OpenAiApi -> 构建ChatModel -> 装配Advisors/MCP工具 -> 返回ChatClient。
 * 使用 {@link ConcurrentHashMap} 缓存已构建的ChatClient实例, 避免重复构建。</p>
 */
@Slf4j
@Service
public class AiClientAssemblyServiceImpl implements AiClientAssemblyService {

    private final ConcurrentHashMap<String, ChatClient> clientCache = new ConcurrentHashMap<>();

    @Resource
    private AiClientConfigMapper aiClientConfigMapper;
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
    private AiClientMapper aiClientMapper;
    @Resource
    @Qualifier("vectorStore")
    private VectorStore vectorStore;
    @Resource
    @Qualifier("executorService")
    private ThreadPoolExecutor executorService;

    /** {@inheritDoc} */
    @Override
    public ChatClient getOrBuildChatClient(String clientId) {
        return clientCache.computeIfAbsent(clientId, this::buildChatClient);
    }

    /** {@inheritDoc} */
    @Override
    public void invalidate(String clientId) {
        clientCache.remove(clientId);
    }

    /**
     * 核心方法 - 从数据库配置构建完整的ChatClient实例。
     * <p>执行步骤:
     * <ol>
     *   <li>加载clientId对应的所有配置关联关系</li>
     *   <li>按目标类型分组, 并行加载Model/Prompt/Advisor/MCP配置</li>
     *   <li>构建OpenAiApi（API地址和密钥）</li>
     *   <li>构建MCP工具回调列表</li>
     *   <li>构建ChatModel（含模型参数和工具回调）</li>
     *   <li>拼接系统提示词</li>
     *   <li>构建Advisor链</li>
     *   <li>组装并返回最终的ChatClient</li>
     * </ol></p>
     *
     * @param clientId 客户端标识ID
     * @return 组装完成的ChatClient实例
     */
    private ChatClient buildChatClient(String clientId) {
        log.info("Building ChatClient for clientId: {}", clientId);

        // 1. Load all config relations for this client
        List<AiClientConfig> configs = aiClientConfigMapper.selectBySource(Constants.TYPE_CLIENT, clientId);
        if (configs == null || configs.isEmpty()) {
            throw new RuntimeException("No config found for clientId: " + clientId);
        }

        // 2. Group target IDs by type
        Map<String, List<String>> targetIdsByType = configs.stream()
                .collect(Collectors.groupingBy(AiClientConfig::getTargetType,
                        Collectors.mapping(AiClientConfig::getTargetId, Collectors.toList())));

        // 3. Parallel load all configuration data
        CompletableFuture<AiClientModel> modelFuture = CompletableFuture.supplyAsync(() -> {
            List<String> modelIds = targetIdsByType.getOrDefault(Constants.TYPE_MODEL, Collections.emptyList());
            return modelIds.isEmpty() ? null : aiClientModelMapper.selectByModelId(modelIds.get(0));
        }, executorService);

        CompletableFuture<List<AiClientSystemPrompt>> promptFuture = CompletableFuture.supplyAsync(() -> {
            List<String> promptIds = targetIdsByType.getOrDefault(Constants.TYPE_PROMPT, Collections.emptyList());
            return promptIds.isEmpty() ? Collections.emptyList() : aiClientSystemPromptMapper.selectByPromptIds(promptIds);
        }, executorService);

        CompletableFuture<List<AiClientAdvisor>> advisorFuture = CompletableFuture.supplyAsync(() -> {
            List<String> advisorIds = targetIdsByType.getOrDefault(Constants.TYPE_ADVISOR, Collections.emptyList());
            return advisorIds.isEmpty() ? Collections.emptyList() : aiClientAdvisorMapper.selectByAdvisorIds(advisorIds);
        }, executorService);

        // Also load MCP tools linked to the model
        CompletableFuture<List<AiClientToolMcp>> mcpFuture = modelFuture.thenComposeAsync(model -> {
            if (model == null) return CompletableFuture.completedFuture(Collections.emptyList());
            // Load MCP tools linked to this model
            List<AiClientConfig> modelConfigs = aiClientConfigMapper.selectBySourceAndTargetType(
                    Constants.TYPE_MODEL, model.getModelId(), Constants.TYPE_TOOL_MCP);
            if (modelConfigs == null || modelConfigs.isEmpty()) return CompletableFuture.completedFuture(Collections.emptyList());
            List<String> mcpIds = modelConfigs.stream().map(AiClientConfig::getTargetId).collect(Collectors.toList());
            return CompletableFuture.completedFuture(aiClientToolMcpMapper.selectByMcpIds(mcpIds));
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
     * 根据MCP配置创建工具回调列表。
     * <p>遍历MCP工具配置, 为每个配置创建McpSyncClient, 然后通过
     * {@link SyncMcpToolCallbackProvider} 统一转换为ToolCallback列表。</p>
     *
     * @param mcpTools MCP工具配置列表
     * @return 工具回调列表, 如果无MCP配置则返回空列表
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
                log.info("MCP client initialized: {} ({})", mcp.getMcpName(), mcp.getTransportType());
            } catch (Exception e) {
                log.error("Failed to create MCP client: {} - {}", mcp.getMcpName(), e.getMessage());
            }
        }

        if (mcpClients.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(
                new SyncMcpToolCallbackProvider(mcpClients.toArray(new McpSyncClient[0])).getToolCallbacks()
        );
    }

    /**
     * 根据传输类型创建MCP客户端。
     * <p>支持SSE和Stdio两种传输方式, 根据配置中的transportType字段自动选择。</p>
     *
     * @param mcp MCP工具配置实体
     * @return 初始化完成的MCP同步客户端
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
     * 创建SSE传输的MCP客户端。
     * <p>从transportConfig JSON中解析baseUri和sseEndpoint, 构建HttpClientSseClientTransport。</p>
     *
     * @param transportConfig 传输配置JSON字符串, 包含baseUri和可选的sseEndpoint
     * @param timeout         请求超时时间
     * @return 初始化完成的MCP同步客户端
     */
    private McpSyncClient createSseMcpClient(String transportConfig, Duration timeout) {
        JSONObject config = JSON.parseObject(transportConfig);
        String baseUri = config.getString("baseUri");
        String sseEndpoint = config.getString("sseEndpoint");

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
     * 创建Stdio传输的MCP客户端。
     * <p>从transportConfig JSON中解析command和args, 构建StdioClientTransport。
     * 配置格式: {"toolName": {"command": "npx", "args": [...]}}</p>
     *
     * @param transportConfig 传输配置JSON字符串, 包含命令和参数
     * @param timeout         请求超时时间
     * @return 初始化完成的MCP同步客户端
     */
    private McpSyncClient createStdioMcpClient(String transportConfig, Duration timeout) {
        JSONObject config = JSON.parseObject(transportConfig);
        // Config structure: { "toolName": { "command": "npx", "args": [...] } }
        String toolName = config.keySet().iterator().next();
        JSONObject toolConfig = config.getJSONObject(toolName);

        String command = toolConfig.getString("command");
        List<String> args = toolConfig.getJSONArray("args").toJavaList(String.class);

        ServerParameters params = ServerParameters.builder(command)
                .args(args.toArray(new String[0]))
                .build();

        McpSyncClient client = McpClient.sync(new StdioClientTransport(params))
                .requestTimeout(timeout).build();
        client.initialize();
        return client;
    }

    /**
     * 根据配置构建Advisor链（ChatMemory/RAG/Logger）。
     * <p>遍历Advisor配置列表, 为每个配置创建对应类型的Advisor实例,
     * 并在末尾始终追加 {@link SimpleLoggerAdvisor} 用于日志记录。</p>
     *
     * @param advisorConfigs Advisor配置列表
     * @return 构建完成的Advisor列表
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
     * 根据配置创建单个Advisor实例。
     * <p>支持的Advisor类型:
     * <ul>
     *   <li>{@code CHAT_MEMORY} - 对话记忆Advisor, 基于消息窗口保持上下文</li>
     *   <li>{@code RAG_ANSWER} - RAG检索增强Advisor, 从向量库检索相关文档注入上下文</li>
     * </ul></p>
     *
     * @param config Advisor配置实体, 包含类型和扩展参数
     * @return 创建的Advisor实例
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
