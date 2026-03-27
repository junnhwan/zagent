package io.wanjune.zagent.chat.assembly.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wanjune.zagent.mcp.McpConfigSyncServiceImpl;
import io.wanjune.zagent.mcp.McpManifestStateHolder;
import io.wanjune.zagent.mcp.McpSyncProperties;
import io.wanjune.zagent.model.dto.McpSyncManifest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class McpConfigSyncServiceImplTest {

    @Test
    void syncIfEnabledUpsertsManagedResourcesAndBindings() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        stubCleanupQueries(jdbcTemplate);
        McpConfigSyncServiceImpl service = createService(jdbcTemplate, true);

        service.syncIfEnabled();

        verify(jdbcTemplate).update(
                startsWith("INSERT INTO ai_client_model"),
                eq("2901"), eq("1001"), eq("gpt-5.4"), eq("openai"), eq(1));

        verify(jdbcTemplate).update(
                startsWith("INSERT INTO ai_client_tool_mcp"),
                eq("5901"),
                eq("test-sse"),
                eq("sse"),
                eq("{\"baseUri\":\"http://127.0.0.1:19090\",\"sseEndpoint\":\"/sse\"}"),
                eq(12),
                eq(1));

        verify(jdbcTemplate).update(
                startsWith("INSERT INTO ai_client_tool_mcp"),
                eq("5902"),
                eq("test-stdio"),
                eq("stdio"),
                eq("{\"filesystem\":{\"command\":\"npx\",\"args\":[\"-y\",\"@modelcontextprotocol/server-filesystem\",\"D:/dev/my_proj/zagent/docs\"],\"env\":{\"MCP_LOG_LEVEL\":\"info\"}}}"),
                eq(8),
                eq(1));

        verify(jdbcTemplate).update(
                startsWith("DELETE FROM ai_client_config"),
                eq("model"), eq("2901"), eq("tool_mcp"));

        verify(jdbcTemplate).update(
                startsWith("INSERT INTO ai_client_config"),
                eq("model"), eq("2901"), eq("tool_mcp"), eq("5901"), eq(null), eq(1));

        verify(jdbcTemplate).update(
                startsWith("INSERT INTO ai_client_config"),
                eq("model"), eq("2901"), eq("tool_mcp"), eq("5902"), eq(null), eq(1));

        verify(jdbcTemplate).update(
                startsWith("DELETE FROM ai_client_config"),
                eq("client"), eq("3901"), eq("model"));

        verify(jdbcTemplate).update(
                startsWith("INSERT INTO ai_client_config"),
                eq("client"), eq("3901"), eq("model"), eq("2901"), eq(null), eq(1));
    }

    @Test
    void syncIfEnabledSkipsWhenDisabled() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        McpConfigSyncServiceImpl service = createService(jdbcTemplate, false);

        service.syncIfEnabled();

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void syncIfEnabledOnlyRunsOnce() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        stubCleanupQueries(jdbcTemplate);
        McpConfigSyncServiceImpl service = createService(jdbcTemplate, true);

        service.syncIfEnabled();
        service.syncIfEnabled();

        verify(jdbcTemplate, times(1)).update(
                startsWith("INSERT INTO ai_client_model"),
                eq("2901"), eq("1001"), eq("gpt-5.4"), eq("openai"), eq(1));
    }

    @Test
    void syncIfEnabledDeletesRetiredMcpBindingsAndUnusedMcpRows() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(startsWith("SELECT source_id, target_id FROM ai_client_config")))
                .thenReturn(List.of(Map.of("source_id", "2902", "target_id", "5999")));
        when(jdbcTemplate.queryForList(eq("SELECT mcp_id FROM ai_client_tool_mcp"), eq(String.class)))
                .thenReturn(List.of("5901", "5902", "5999"));
        when(jdbcTemplate.queryForObject(
                startsWith("SELECT COUNT(1) FROM ai_client_config"),
                eq(Integer.class),
                eq("tool_mcp"),
                eq("5999")))
                .thenReturn(0);

        McpConfigSyncServiceImpl service = createService(jdbcTemplate, true);

        service.syncIfEnabled();

        verify(jdbcTemplate).update(
                startsWith("DELETE FROM ai_client_config WHERE source_type = ? AND source_id = ? AND target_type = ? AND target_id = ?"),
                eq("model"), eq("2902"), eq("tool_mcp"), eq("5999"));

        verify(jdbcTemplate).update(
                startsWith("DELETE FROM ai_client_tool_mcp WHERE mcp_id = ?"),
                eq("5999"));
    }

    private McpConfigSyncServiceImpl createService(JdbcTemplate jdbcTemplate, boolean enabled) {
        McpSyncProperties properties = new McpSyncProperties();
        properties.setEnabled(enabled);
        properties.setManifest(manifest());

        ObjectMapper objectMapper = new ObjectMapper();
        McpManifestStateHolder stateHolder = new McpManifestStateHolder(properties, objectMapper);
        stateHolder.initialize();

        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("test.mcp.base-uri", "http://127.0.0.1:19090");

        return new McpConfigSyncServiceImpl(jdbcTemplate, properties, stateHolder, environment);
    }

    private McpSyncManifest manifest() {
        McpSyncManifest manifest = new McpSyncManifest();

        McpSyncManifest.ModelConfig model = new McpSyncManifest.ModelConfig();
        model.setModelId("2901");
        model.setApiId("1001");
        model.setModelName("gpt-5.4");
        model.setModelType("openai");
        model.setStatus(1);
        manifest.setModels(List.of(model));

        McpSyncManifest.McpToolConfig sseMcp = new McpSyncManifest.McpToolConfig();
        sseMcp.setMcpId("5901");
        sseMcp.setMcpName("test-sse");
        sseMcp.setTransportType("sse");
        sseMcp.setTransportConfig("{\"baseUri\":\"${test.mcp.base-uri:http://127.0.0.1:19090}\",\"sseEndpoint\":\"/sse\"}");
        sseMcp.setRequestTimeout(12);
        sseMcp.setStatus(1);

        McpSyncManifest.McpToolConfig stdioMcp = new McpSyncManifest.McpToolConfig();
        stdioMcp.setMcpId("5902");
        stdioMcp.setMcpName("test-stdio");
        stdioMcp.setTransportType("stdio");
        stdioMcp.setTransportConfig("{\"filesystem\":{\"command\":\"npx\",\"args\":[\"-y\",\"@modelcontextprotocol/server-filesystem\",\"D:/dev/my_proj/zagent/docs\"],\"env\":{\"MCP_LOG_LEVEL\":\"info\"}}}");
        stdioMcp.setRequestTimeout(8);
        stdioMcp.setStatus(1);
        manifest.setMcps(List.of(sseMcp, stdioMcp));

        McpSyncManifest.BindingConfig modelBinding = new McpSyncManifest.BindingConfig();
        modelBinding.setSourceType("model");
        modelBinding.setSourceId("2901");
        modelBinding.setTargetType("tool_mcp");
        modelBinding.setTargetIds(List.of("5901", "5902"));
        modelBinding.setStatus(1);

        McpSyncManifest.BindingConfig clientBinding = new McpSyncManifest.BindingConfig();
        clientBinding.setSourceType("client");
        clientBinding.setSourceId("3901");
        clientBinding.setTargetType("model");
        clientBinding.setTargetIds(List.of("2901", "2901"));
        clientBinding.setStatus(1);

        manifest.setBindings(List.of(modelBinding, clientBinding));
        return manifest;
    }

    private void stubCleanupQueries(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForList(startsWith("SELECT source_id, target_id FROM ai_client_config")))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForList(eq("SELECT mcp_id FROM ai_client_tool_mcp"), eq(String.class)))
                .thenReturn(List.of("5901", "5902"));
    }
}
