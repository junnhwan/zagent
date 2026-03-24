package io.wanjune.zagent.agent.strategy.support;

import io.wanjune.zagent.agent.strategy.impl.AutoExecuteStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutoSupervisionParserTest {

    private final AutoSupervisionParser parser = new AutoSupervisionParser();

    @Test
    void parseSupervisionDecisionSupportsJson() {
        String json = """
                {
                  "decision": "PASS",
                  "improvement": "补充细节"
                }
                """;

        AutoExecuteStrategy.SupervisionDecision decision = parser.parseSupervisionDecision(json);

        assertThat(decision.decision()).isEqualTo("PASS");
        assertThat(decision.improvement()).isEqualTo("补充细节");
        assertThat(decision.source()).isEqualTo("json");
    }

    @Test
    void parseSupervisionDecisionSupportsTextFallback() {
        String text = "是否通过: FAIL\n改进建议: 无";

        AutoExecuteStrategy.SupervisionDecision decision = parser.parseSupervisionDecision(text);

        assertThat(decision.decision()).isEqualTo("FAIL");
        assertThat(decision.improvement()).contains("无");
        assertThat(decision.source()).isEqualTo("text-fallback");
    }
}
