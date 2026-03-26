package io.wanjune.zagent.chat.assembly.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wanjune.zagent.chat.assembly.AiClientAssemblyService;
import io.wanjune.zagent.mcp.McpConfigSyncService;
import io.wanjune.zagent.mcp.McpManifestStateHolder;
import io.wanjune.zagent.mcp.McpModeAdminServiceImpl;
import io.wanjune.zagent.mcp.McpSyncProperties;
import io.wanjune.zagent.mcp.McpTransportConfigParser;
import io.wanjune.zagent.mcp.McpTransportConfigParserImpl;
import io.wanjune.zagent.model.dto.McpRuntimeState;
import io.wanjune.zagent.model.dto.McpSyncManifest;
import io.wanjune.zagent.model.vo.McpModeStatusVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpModeAdminServiceImplTest {

    private final McpTransportConfigParser parser = new McpTransportConfigParserImpl();

    @Test
    void getCurrentStatusReadsCurrentManifestFromStateHolder() {
        AiClientAssemblyService assemblyService = Mockito.mock(AiClientAssemblyService.class);
        Mockito.when(assemblyService.getMcpRuntimeStates()).thenReturn(Map.of());

        McpSyncProperties properties = new McpSyncProperties();
        properties.setManifest(manifest(
                List.of(),
                List.of(),
                List.of(binding("client", "3006", "model", List.of("2002")))
        ));
        McpManifestStateHolder stateHolder = new McpManifestStateHolder(properties, new ObjectMapper());
        stateHolder.initialize();

        McpModeAdminServiceImpl service = new McpModeAdminServiceImpl(
                stateHolder,
                Mockito.mock(McpConfigSyncService.class),
                assemblyService,
                parser
        );

        McpModeStatusVO status = service.getCurrentStatus();

        assertThat(status.getCurrentMode()).isEqualTo("stdio");
        assertThat(status.getCurrentModelId()).isEqualTo("2002");
        assertThat(status.getOptions()).hasSize(3);
    }

    @Test
    void getCurrentStatusIncludesActiveMcpTransportAndLastError() {
        AiClientAssemblyService assemblyService = Mockito.mock(AiClientAssemblyService.class);
        Mockito.when(assemblyService.getMcpRuntimeStates()).thenReturn(Map.of(
                "5003", new McpRuntimeState(false, "SSE connection refused", Instant.parse("2026-03-21T08:00:00Z"))
        ));

        McpSyncProperties properties = new McpSyncProperties();
        properties.setManifest(manifest(
                List.of(model("2005", "1001", "gpt-5.4", "openai")),
                List.of(mcp("5003", "amap-sse", "sse", "{\"baseUri\":\"http://127.0.0.1:18081/\",\"sseEndpoint\":\"sse/\"}", 20)),
                List.of(
                        binding("model", "2005", "tool_mcp", List.of("5003")),
                        binding("client", "3006", "model", List.of("2005"))
                )
        ));
        McpManifestStateHolder stateHolder = new McpManifestStateHolder(properties, new ObjectMapper());
        stateHolder.initialize();

        McpModeAdminServiceImpl service = new McpModeAdminServiceImpl(
                stateHolder,
                Mockito.mock(McpConfigSyncService.class),
                assemblyService,
                parser
        );

        McpModeStatusVO status = service.getCurrentStatus();

        assertThat(status.getCurrentMode()).isEqualTo("amap");
        assertThat(status.getActiveMcps()).hasSize(1);
        assertThat(status.getActiveMcps().get(0).getMcpId()).isEqualTo("5003");
        assertThat(status.getActiveMcps().get(0).getTransportType()).isEqualTo("sse");
        assertThat(status.getActiveMcps().get(0).getTransportSummary()).isEqualTo("http://127.0.0.1:18081/sse");
        assertThat(status.getActiveMcps().get(0).getReady()).isFalse();
        assertThat(status.getActiveMcps().get(0).getLastError()).isEqualTo("SSE connection refused");
    }

    private McpSyncManifest manifest(List<McpSyncManifest.ModelConfig> models,
                                     List<McpSyncManifest.McpToolConfig> mcps,
                                     List<McpSyncManifest.BindingConfig> bindings) {
        McpSyncManifest manifest = new McpSyncManifest();
        manifest.setModels(models);
        manifest.setMcps(mcps);
        manifest.setBindings(bindings);
        return manifest;
    }

    private McpSyncManifest.ModelConfig model(String modelId, String apiId, String modelName, String modelType) {
        McpSyncManifest.ModelConfig model = new McpSyncManifest.ModelConfig();
        model.setModelId(modelId);
        model.setApiId(apiId);
        model.setModelName(modelName);
        model.setModelType(modelType);
        model.setStatus(1);
        return model;
    }

    private McpSyncManifest.McpToolConfig mcp(String mcpId, String mcpName, String transportType, String transportConfig, int timeout) {
        McpSyncManifest.McpToolConfig mcp = new McpSyncManifest.McpToolConfig();
        mcp.setMcpId(mcpId);
        mcp.setMcpName(mcpName);
        mcp.setTransportType(transportType);
        mcp.setTransportConfig(transportConfig);
        mcp.setRequestTimeout(timeout);
        mcp.setStatus(1);
        return mcp;
    }

    private McpSyncManifest.BindingConfig binding(String sourceType, String sourceId, String targetType, List<String> targetIds) {
        McpSyncManifest.BindingConfig binding = new McpSyncManifest.BindingConfig();
        binding.setSourceType(sourceType);
        binding.setSourceId(sourceId);
        binding.setTargetType(targetType);
        binding.setTargetIds(targetIds);
        binding.setStatus(1);
        return binding;
    }
}
