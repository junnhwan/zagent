package io.wanjune.zagent.agent.strategy;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlowExecuteStrategyTest {

    @Test
    void limitExecutionStepsRespectsMaxStep() {
        Map<Integer, String> steps = new LinkedHashMap<>();
        steps.put(1, "step-1");
        steps.put(2, "step-2");
        steps.put(3, "step-3");
        steps.put(4, "step-4");

        Map<Integer, String> limited = FlowExecuteStrategy.limitExecutionSteps(steps, 3);

        assertThat(limited).containsExactly(
                Map.entry(1, "step-1"),
                Map.entry(2, "step-2"),
                Map.entry(3, "step-3")
        );
    }
}
