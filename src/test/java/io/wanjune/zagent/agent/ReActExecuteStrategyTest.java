package io.wanjune.zagent.agent;

import io.wanjune.zagent.agent.strategy.support.AgentToolMapBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReActExecuteStrategyTest {

    @Test
    void buildToolMapShouldUseToolDefinitionName() {
        AgentToolMapBuilder builder = new AgentToolMapBuilder(null);

        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        when(definition.name()).thenReturn("amap_weather");

        Map<String, ToolCallback> result = builder.buildToolMap(List.of(callback));

        assertThat(result).containsKey("amap_weather");
    }
}
