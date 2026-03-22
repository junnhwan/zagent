package io.wanjune.zagent.service.impl;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.wanjune.zagent.mapper.AiAgentFlowConfigMapper;
import io.wanjune.zagent.mapper.AiClientAdvisorMapper;
import io.wanjune.zagent.mapper.AiClientApiMapper;
import io.wanjune.zagent.mapper.AiClientModelMapper;
import io.wanjune.zagent.mapper.AiClientSystemPromptMapper;
import io.wanjune.zagent.mapper.AiClientToolMcpMapper;
import io.wanjune.zagent.model.dto.McpRuntimeState;
import io.wanjune.zagent.model.entity.AiAgentFlowConfig;
import io.wanjune.zagent.model.entity.AiClientAdvisor;
import io.wanjune.zagent.model.entity.AiClientApi;
import io.wanjune.zagent.model.entity.AiClientModel;
import io.wanjune.zagent.model.entity.AiClientSystemPrompt;
import io.wanjune.zagent.model.entity.AiClientToolMcp;
import io.wanjune.zagent.service.AiClientAssemblyService;
import io.wanjune.zagent.service.McpBindingResolver;
import io.wanjune.zagent.service.McpConfigSyncService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
public class AiClientAssemblyServiceImpl implements AiClientAssemblyService, org.springframework.beans.factory.DisposableBean {

    private final ConcurrentHashMap<String, ChatClient> clientCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, McpRuntimeState> mcpRuntimeStates = new ConcurrentHashMap<>();
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
    @Qualifier("executorService")
    private ThreadPoolExecutor executorService;
    @Resource
    private McpConfigSyncService mcpConfigSyncService;
    @Resource
    private McpBindingResolver mcpBindingResolver;
    @Resource
    private AiClientModelFactory aiClientModelFactory;
    @Resource
    private AiClientAdvisorFactory aiClientAdvisorFactory;
    @Resource
    private AiClientMcpToolFactory aiClientMcpToolFactory;

    @Override
    public ChatClient getOrBuildChatClient(String clientId) {
        return clientCache.computeIfAbsent(clientId, this::buildChatClient);
    }

    @Override
    public void invalidate(String clientId) {
        clientCache.remove(clientId);
        log.info("ChatClient cache invalidated for clientId: {}", clientId);
    }

    @Override
    public Map<String, McpRuntimeState> getMcpRuntimeStates() {
        return Collections.unmodifiableMap(mcpRuntimeStates);
    }

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

    @PostConstruct
    public void init() {
        executorService.execute(() -> {
            try {
                mcpConfigSyncService.syncIfEnabled();
                warmUpAll();
            } catch (Exception e) {
                log.warn("Warmup skipped due to initialization failure: {}", e.getMessage());
            }
        });
    }

    @Override
    public void warmUpAll() {
        List<AiAgentFlowConfig> flowConfigs = aiAgentFlowConfigMapper.selectAll();
        if (flowConfigs == null || flowConfigs.isEmpty()) {
            return;
        }

        int success = 0;
        for (AiAgentFlowConfig config : flowConfigs) {
            try {
                if (config.getClientId() != null) {
                    getOrBuildChatClient(config.getClientId());
                    success++;
                }
            } catch (Exception e) {
                log.warn("Warmup failed for clientId {}: {}", config.getClientId(), e.getMessage());
            }
        }
        log.info("预热完成: {}/{} 个ChatClient构建成功", success, flowConfigs.size());
    }

    private ChatClient buildChatClient(String clientId) {
        log.info("Building ChatClient for clientId: {}", clientId);

        var bindingResolution = mcpBindingResolver.resolve(clientId);

        CompletableFuture<AiClientModel> modelFuture = CompletableFuture.supplyAsync(
                () -> aiClientModelMapper.selectByModelId(bindingResolution.modelId()), executorService);

        CompletableFuture<List<AiClientSystemPrompt>> promptFuture = CompletableFuture.supplyAsync(() -> {
            List<String> promptIds = bindingResolution.promptIds();
            return promptIds.isEmpty() ? Collections.emptyList() : aiClientSystemPromptMapper.selectByPromptIds(promptIds);
        }, executorService);

        CompletableFuture<List<AiClientAdvisor>> advisorFuture = CompletableFuture.supplyAsync(() -> {
            List<String> advisorIds = bindingResolution.advisorIds();
            return advisorIds.isEmpty() ? Collections.emptyList() : aiClientAdvisorMapper.selectByAdvisorIds(advisorIds);
        }, executorService);

        CompletableFuture<List<AiClientToolMcp>> mcpFuture = CompletableFuture.supplyAsync(() -> {
            List<String> mcpIds = bindingResolution.mcpIds();
            return mcpIds.isEmpty() ? Collections.emptyList() : aiClientToolMcpMapper.selectByMcpIds(mcpIds);
        }, executorService);

        AiClientModel modelConfig = modelFuture.join();
        List<AiClientSystemPrompt> prompts = promptFuture.join();
        List<AiClientAdvisor> advisors = advisorFuture.join();
        List<AiClientToolMcp> mcpTools = mcpFuture.join();

        if (modelConfig == null) {
            throw new RuntimeException("No model config found for clientId: " + clientId);
        }

        AiClientApi apiConfig = aiClientApiMapper.selectByApiId(modelConfig.getApiId());
        if (apiConfig == null) {
            throw new RuntimeException("No API config found for apiId: " + modelConfig.getApiId());
        }

        OpenAiApi openAiApi = aiClientModelFactory.createOpenAiApi(apiConfig);
        List<ToolCallback> toolCallbacks = aiClientMcpToolFactory.buildMcpToolCallbacks(mcpTools, mcpClientPool, mcpRuntimeStates);
        OpenAiChatModel chatModel = aiClientModelFactory.createChatModel(modelConfig, openAiApi, toolCallbacks);
        String systemPrompt = aiClientAdvisorFactory.buildSystemPrompt(prompts);
        List<Advisor> advisorList = aiClientAdvisorFactory.buildAdvisors(advisors);

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

    static String formatMcpBindingLabel(AiClientToolMcp mcp) {
        return AiClientMcpToolFactory.formatMcpBindingLabel(mcp);
    }
}
