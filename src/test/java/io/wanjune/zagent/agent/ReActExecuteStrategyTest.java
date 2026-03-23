package io.wanjune.zagent.agent;

import io.wanjune.zagent.agent.strategy.impl.ReActExecuteStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReActExecuteStrategyTest {

    @Test
    void buildToolMapShouldUseToolDefinitionName() throws Exception {
        ReActExecuteStrategy strategy = new ReActExecuteStrategy();
        var method = ReActExecuteStrategy.class.getDeclaredMethod("buildToolMap", java.util.List.class);
        method.setAccessible(true);

        org.springframework.ai.tool.ToolCallback callback = org.mockito.Mockito.mock(org.springframework.ai.tool.ToolCallback.class);
        org.springframework.ai.tool.definition.ToolDefinition definition = org.mockito.Mockito.mock(org.springframework.ai.tool.definition.ToolDefinition.class);
        org.mockito.Mockito.when(callback.getToolDefinition()).thenReturn(definition);
        org.mockito.Mockito.when(definition.name()).thenReturn("amap_weather");

        Object result = method.invoke(strategy, java.util.List.of(callback));

        assertThat(result).asString().contains("amap_weather");
    }

    @Test
    void parsePositiveIntFallsBackWhenInvalid() throws Exception {
        ReActExecuteStrategy strategy = new ReActExecuteStrategy();
        var method = ReActExecuteStrategy.class.getDeclaredMethod("parsePositiveInt", java.util.Map.class, String.class, int.class);
        method.setAccessible(true);

        Object result = method.invoke(strategy, java.util.Map.of("maxIterations", "bad"), "maxIterations", 6);

        assertThat(result).isEqualTo(6);
    }

    @Test
    void parsePositiveIntUsesConfiguredValue() throws Exception {
        ReActExecuteStrategy strategy = new ReActExecuteStrategy();
        var method = ReActExecuteStrategy.class.getDeclaredMethod("parsePositiveInt", java.util.Map.class, String.class, int.class);
        method.setAccessible(true);

        Object result = method.invoke(strategy, java.util.Map.of("maxIterations", 8), "maxIterations", 6);

        assertThat(result).isEqualTo(8);
    }
}
