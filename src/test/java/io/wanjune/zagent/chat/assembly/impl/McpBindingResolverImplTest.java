package io.wanjune.zagent.chat.assembly.impl;

import io.wanjune.zagent.common.Constants;
import io.wanjune.zagent.mapper.AiClientConfigMapper;
import io.wanjune.zagent.model.dto.AiClientBindingResolution;
import io.wanjune.zagent.model.entity.AiClientConfig;
import io.wanjune.zagent.mcp.McpBindingResolver;
import io.wanjune.zagent.mcp.impl.McpBindingResolverImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpBindingResolverImplTest {

    private final AiClientConfigMapper aiClientConfigMapper = Mockito.mock(AiClientConfigMapper.class);
    private final McpBindingResolver resolver = new McpBindingResolverImpl(aiClientConfigMapper);

    @Test
    void resolveReturnsModelPromptAdvisorAndDistinctMcpBindings() {
        Mockito.when(aiClientConfigMapper.selectBySource(Constants.TYPE_CLIENT, "3006"))
                .thenReturn(List.of(
                        config(Constants.TYPE_CLIENT, "3006", Constants.TYPE_MODEL, "2005"),
                        config(Constants.TYPE_CLIENT, "3006", Constants.TYPE_PROMPT, "6001"),
                        config(Constants.TYPE_CLIENT, "3006", Constants.TYPE_PROMPT, "6001"),
                        config(Constants.TYPE_CLIENT, "3006", Constants.TYPE_ADVISOR, "7001")
                ));
        Mockito.when(aiClientConfigMapper.selectBySourceAndTargetType(Constants.TYPE_MODEL, "2005", Constants.TYPE_TOOL_MCP))
                .thenReturn(List.of(
                        config(Constants.TYPE_MODEL, "2005", Constants.TYPE_TOOL_MCP, "5001"),
                        config(Constants.TYPE_MODEL, "2005", Constants.TYPE_TOOL_MCP, "5003"),
                        config(Constants.TYPE_MODEL, "2005", Constants.TYPE_TOOL_MCP, "5001")
                ));

        AiClientBindingResolution resolution = resolver.resolve("3006");

        assertThat(resolution.clientId()).isEqualTo("3006");
        assertThat(resolution.modelId()).isEqualTo("2005");
        assertThat(resolution.promptIds()).containsExactly("6001");
        assertThat(resolution.advisorIds()).containsExactly("7001");
        assertThat(resolution.mcpIds()).containsExactly("5001", "5003");
    }

    @Test
    void resolveThrowsWhenClientHasNoEnabledBindings() {
        Mockito.when(aiClientConfigMapper.selectBySource(Constants.TYPE_CLIENT, "3999"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve("3999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3999");
    }

    @Test
    void resolveThrowsWhenClientHasNoModelBinding() {
        Mockito.when(aiClientConfigMapper.selectBySource(Constants.TYPE_CLIENT, "3007"))
                .thenReturn(List.of(
                        config(Constants.TYPE_CLIENT, "3007", Constants.TYPE_PROMPT, "6002")
                ));

        assertThatThrownBy(() -> resolver.resolve("3007"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3007")
                .hasMessageContaining("model");
    }

    private static AiClientConfig config(String sourceType, String sourceId, String targetType, String targetId) {
        AiClientConfig config = new AiClientConfig();
        config.setSourceType(sourceType);
        config.setSourceId(sourceId);
        config.setTargetType(targetType);
        config.setTargetId(targetId);
        config.setStatus(1);
        return config;
    }
}
