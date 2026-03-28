package io.wanjune.zagent.chat.assembly.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.wanjune.zagent.model.dto.McpRuntimeState;
import io.wanjune.zagent.model.entity.AiClientToolMcp;
import io.wanjune.zagent.model.enums.TransportTypeEnum;
import io.wanjune.zagent.mcp.McpTransportConfigParser;
import io.wanjune.zagent.mcp.impl.McpTransportConfigParserImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClientMcpToolFactory {

    private final McpTransportConfigParser mcpTransportConfigParser;
    private final ObjectMapper objectMapper;

    public List<ToolCallback> buildMcpToolCallbacks(List<AiClientToolMcp> mcpTools,
                                                    List<McpSyncClient> mcpClientPool,
                                                    Map<String, McpRuntimeState> mcpRuntimeStates) {
        return mcpTools.stream().flatMap(mcp -> {
            String bindingLabel = formatMcpBindingLabel(mcp);
            try {
                McpSyncClient client = createMcpClient(mcp);
                mcpClientPool.add(client);
                mcpRuntimeStates.put(mcp.getMcpId(), new McpRuntimeState(true, null, Instant.now()));
                SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(List.of(client));
                List<String> bindingLabels = List.of(bindingLabel);
                return Arrays.stream(provider.getToolCallbacks())
                        .map(callback -> wrapToolCallback(callback, bindingLabels));
            } catch (Exception e) {
                mcpRuntimeStates.put(mcp.getMcpId(), new McpRuntimeState(false, e.getMessage(), Instant.now()));
                log.error("Failed to create MCP client: {} - {}", mcp.getMcpName(), e.getMessage());
                return java.util.stream.Stream.<ToolCallback>empty();
            }
        }).toList();
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

    private McpSyncClient createMcpClient(AiClientToolMcp mcp) {
        TransportTypeEnum transportType = TransportTypeEnum.of(mcp.getTransportType());
        Duration timeout = Duration.ofMinutes(mcp.getRequestTimeout() != null ? mcp.getRequestTimeout() : 180);
        if (transportType == TransportTypeEnum.SSE) {
            return createSseMcpClient(mcp.getTransportConfig(), timeout);
        }
        return createStdioMcpClient(mcp.getTransportConfig(), timeout);
    }

    private McpSyncClient createSseMcpClient(String transportConfig, Duration timeout) {
        var config = mcpTransportConfigParser.parseSse(transportConfig);
        String baseUri = McpTransportConfigParserImpl.normalizeSseBaseUri(config.baseUri());
        String sseEndpoint = McpTransportConfigParserImpl.normalizeSseEndpoint(config.sseEndpoint());
        log.info("Initializing SSE MCP transport, baseUri: {}, sseEndpoint: {}", baseUri, sseEndpoint);
        McpSyncClient client = McpClient.sync(
                        HttpClientSseClientTransport.builder(baseUri)
                                .sseEndpoint(sseEndpoint)
                                .build())
                .requestTimeout(timeout).build();
        client.initialize();
        return client;
    }

    private McpSyncClient createStdioMcpClient(String transportConfig, Duration timeout) {
        ServerParameters params = mcpTransportConfigParser.toServerParameters(
                mcpTransportConfigParser.parseStdio(transportConfig));
        McpSyncClient client = McpClient.sync(new StdioClientTransport(params))
                .requestTimeout(timeout).build();
        client.initialize();
        return client;
    }

    public static String formatMcpBindingLabel(AiClientToolMcp mcp) {
        return mcp.getMcpName() + "[" + mcp.getTransportType() + "]";
    }

    public static String abbreviateForLog(String text) {
        if (text == null) {
            return "null";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
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
}
