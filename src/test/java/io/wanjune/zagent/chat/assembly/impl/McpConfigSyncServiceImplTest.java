package io.wanjune.zagent.chat.assembly.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wanjune.zagent.mcp.McpConfigSyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class McpConfigSyncServiceImplTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(McpConfigSyncServiceImpl.class, TestConfig.class)
            .withPropertyValues(
                    "zagent.mcp.sync.enabled=true",
                    "zagent.mcp.sync.location=classpath:mcp-tools-test.json",
                    "test.mcp.base-uri=http://127.0.0.1:19090"
            );

    @Test
    void syncIfEnabledUpsertsManagedResourcesAndBindings() {
        contextRunner.run(context -> {
            McpConfigSyncServiceImpl service = context.getBean(McpConfigSyncServiceImpl.class);
            JdbcTemplate jdbcTemplate = context.getBean("mysqlJdbcTemplate", JdbcTemplate.class);

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
                    startsWith("DELETE FROM ai_client_config"),
                    eq("model"), eq("2901"), eq("tool_mcp"));

            verify(jdbcTemplate).update(
                    startsWith("INSERT INTO ai_client_config"),
                    eq("model"), eq("2901"), eq("tool_mcp"), eq("5901"), eq(null), eq(1));

            verify(jdbcTemplate).update(
                    startsWith("DELETE FROM ai_client_config"),
                    eq("client"), eq("3901"), eq("model"));

            verify(jdbcTemplate).update(
                    startsWith("INSERT INTO ai_client_config"),
                    eq("client"), eq("3901"), eq("model"), eq("2901"), eq(null), eq(1));
        });
    }

    @Test
    void syncIfEnabledSkipsWhenDisabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(McpConfigSyncServiceImpl.class, TestConfig.class)
                .withPropertyValues(
                        "zagent.mcp.sync.enabled=false",
                        "zagent.mcp.sync.location=classpath:mcp-tools-test.json"
                )
                .run(context -> {
                    McpConfigSyncServiceImpl service = context.getBean(McpConfigSyncServiceImpl.class);
                    JdbcTemplate jdbcTemplate = context.getBean("mysqlJdbcTemplate", JdbcTemplate.class);

                    Mockito.clearInvocations(jdbcTemplate);

                    service.syncIfEnabled();

                    verifyNoInteractions(jdbcTemplate);
                });
    }

    @Test
    void syncIfEnabledOnlyRunsOnce() {
        contextRunner.run(context -> {
            McpConfigSyncServiceImpl service = context.getBean(McpConfigSyncServiceImpl.class);
            JdbcTemplate jdbcTemplate = context.getBean("mysqlJdbcTemplate", JdbcTemplate.class);

            service.syncIfEnabled();
            service.syncIfEnabled();

            verify(jdbcTemplate, times(1)).update(
                    startsWith("INSERT INTO ai_client_model"),
                    eq("2901"), eq("1001"), eq("gpt-5.4"), eq("openai"), eq(1));
        });
    }

    @Configuration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean("mysqlJdbcTemplate")
        JdbcTemplate mysqlJdbcTemplate() {
            return Mockito.mock(JdbcTemplate.class);
        }
    }
}
