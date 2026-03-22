package io.wanjune.zagent.agent.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutoExecuteStrategyTest {

    @Test
    void parseSupervisionDecisionSupportsJson() {
        String json = """
                {
                  "decision": "FAIL",
                  "score": 4,
                  "match": "不完整",
                  "issues": "缺少量化成果",
                  "improvement": "补充可量化成果和追问点"
                }
                """;

        AutoExecuteStrategy.SupervisionDecision decision = AutoExecuteStrategy.parseSupervisionDecision(json);

        assertThat(decision.decision()).isEqualTo("FAIL");
        assertThat(decision.improvement()).isEqualTo("补充可量化成果和追问点");
        assertThat(decision.source()).isEqualTo("json");
    }

    @Test
    void parseSupervisionDecisionFallsBackToText() {
        String text = """
                需求匹配度: 一般
                内容完整性: 不足
                问题识别: 缺少关键字段
                改进建议: 补充技术实现和量化成果
                是否通过: FAIL
                """;

        AutoExecuteStrategy.SupervisionDecision decision = AutoExecuteStrategy.parseSupervisionDecision(text);

        assertThat(decision.decision()).isEqualTo("FAIL");
        assertThat(decision.improvement()).isEqualTo("补充技术实现和量化成果");
        assertThat(decision.source()).isEqualTo("text-fallback");
    }
}
