package io.wanjune.zagent.chat.assembly.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wanjune.zagent.model.dto.McpRuntimeState;
import io.wanjune.zagent.model.vo.McpModeStatusVO;
import io.wanjune.zagent.mcp.McpTransportConfigParser;
import io.wanjune.zagent.chat.assembly.AiClientAssemblyService;
import io.wanjune.zagent.mcp.McpConfigSyncService;
import io.wanjune.zagent.mcp.impl.McpModeAdminServiceImpl;
import io.wanjune.zagent.mcp.impl.McpTransportConfigParserImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class McpModeAdminServiceImplTest {

    private final McpTransportConfigParser parser = new McpTransportConfigParserImpl();

    @Test
    void getCurrentStatusSupportsUtf8BomManifest() throws Exception {
        Path tempFile = Files.createTempFile("mcp-tools-bom", ".json");
        String json = """
                {
                  "models": [],
                  "mcps": [],
                  "bindings": [
                    {
                      "sourceType": "client",
                      "sourceId": "3006",
                      "targetType": "model",
                      "targetIds": ["2002"],
                      "status": 1
                    }
                  ]
                }
                """;

        Files.writeString(tempFile, "\uFEFF" + json, StandardCharsets.UTF_8);

        AiClientAssemblyService assemblyService = Mockito.mock(AiClientAssemblyService.class);
        Mockito.when(assemblyService.getMcpRuntimeStates()).thenReturn(Map.of());

        McpModeAdminServiceImpl service = new McpModeAdminServiceImpl(
                new ObjectMapper(),
                new DefaultResourceLoader(),
                Mockito.mock(McpConfigSyncService.class),
                assemblyService,
                parser
        );
        ReflectionTestUtils.setField(service, "configLocation", tempFile.toUri().toString());

        McpModeStatusVO status = service.getCurrentStatus();

        assertThat(status.getCurrentMode()).isEqualTo("stdio");
        assertThat(status.getCurrentModelId()).isEqualTo("2002");
        assertThat(status.getOptions()).hasSize(3);
    }

    @Test
    void getCurrentStatusIncludesActiveMcpTransportAndLastError() throws Exception {
        Path tempFile = Files.createTempFile("mcp-tools-runtime", ".json");
        String sseTransportConfig = new ObjectMapper().writeValueAsString(
                "{\"baseUri\":\"http://127.0.0.1:18081/\",\"sseEndpoint\":\"sse/\"}");

        Files.writeString(tempFile, """
                {
                  "models": [
                    {"modelId":"2005","apiId":"1001","modelName":"gpt-5.4","modelType":"openai","status":1}
                  ],
                  "mcps": [
                    {"mcpId":"5003","mcpName":"amap-sse","transportType":"sse","transportConfig":%s,"requestTimeout":20,"status":1}
                  ],
                  "bindings": [
                    {"sourceType":"model","sourceId":"2005","targetType":"tool_mcp","targetIds":["5003"],"status":1},
                    {"sourceType":"client","sourceId":"3006","targetType":"model","targetIds":["2005"],"status":1}
                  ]
                }
                """.formatted(sseTransportConfig), StandardCharsets.UTF_8);

        AiClientAssemblyService assemblyService = Mockito.mock(AiClientAssemblyService.class);
        Mockito.when(assemblyService.getMcpRuntimeStates()).thenReturn(Map.of(
                "5003", new McpRuntimeState(false, "SSE connection refused", Instant.parse("2026-03-21T08:00:00Z"))
        ));

        McpModeAdminServiceImpl service = new McpModeAdminServiceImpl(
                new ObjectMapper(),
                new DefaultResourceLoader(),
                Mockito.mock(McpConfigSyncService.class),
                assemblyService,
                parser
        );
        ReflectionTestUtils.setField(service, "configLocation", tempFile.toUri().toString());

        McpModeStatusVO status = service.getCurrentStatus();

        assertThat(status.getCurrentMode()).isEqualTo("amap");
        assertThat(status.getActiveMcps()).hasSize(1);
        assertThat(status.getActiveMcps().get(0).getMcpId()).isEqualTo("5003");
        assertThat(status.getActiveMcps().get(0).getTransportType()).isEqualTo("sse");
        assertThat(status.getActiveMcps().get(0).getTransportSummary()).isEqualTo("http://127.0.0.1:18081/sse");
        assertThat(status.getActiveMcps().get(0).getReady()).isFalse();
        assertThat(status.getActiveMcps().get(0).getLastError()).isEqualTo("SSE connection refused");
    }
}
