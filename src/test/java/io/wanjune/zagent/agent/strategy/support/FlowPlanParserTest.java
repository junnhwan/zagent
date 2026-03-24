package io.wanjune.zagent.agent.strategy.support;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlowPlanParserTest {

    private final FlowPlanParser parser = new FlowPlanParser();

    @Test
    void limitExecutionStepsRespectsMaxStep() {
        Map<Integer, String> steps = new LinkedHashMap<>();
        steps.put(1, "step-1");
        steps.put(2, "step-2");
        steps.put(3, "step-3");
        steps.put(4, "step-4");

        Map<Integer, String> limited = FlowPlanParser.limitExecutionSteps(steps, 3);

        assertThat(limited).containsExactly(
                Map.entry(1, "step-1"),
                Map.entry(2, "step-2"),
                Map.entry(3, "step-3")
        );
    }

    @Test
    void parseExecutionStepsSupportsStructuredJsonPlan() {
        String json = """
                {
                  "summary": "test",
                  "steps": [
                    {
                      "step": 1,
                      "goal": "查询天气",
                      "tool": "amap_weather",
                      "dependsOn": [],
                      "instruction": "查询杭州今天天气，返回温度和天气情况"
                    },
                    {
                      "step": 2,
                      "goal": "搜索咖啡店",
                      "tool": "amap_poi_search",
                      "dependsOn": [1],
                      "instruction": "搜索西湖附近咖啡店，返回名称和地址"
                    }
                  ]
                }
                """;

        Map<Integer, String> steps = parser.parseExecutionSteps(json);

        assertThat(steps).hasSize(2);
        assertThat(steps.get(1)).contains("第1步- 查询天气", "amap_weather", "查询杭州今天天气");
        assertThat(steps.get(2)).contains("第2步- 搜索咖啡店", "[1]", "搜索西湖附近咖啡店");
    }

    @Test
    void extractJsonObjectSupportsWrappedJsonContent() {
        String wrapped = "这里是解释文字\n```json\n{\"summary\":\"x\",\"steps\":[]}\n```\n更多说明";

        String extracted = FlowPlanParser.extractJsonObject(wrapped);

        assertThat(extracted).isEqualTo("{\"summary\":\"x\",\"steps\":[]}");
    }

    @Test
    void parseExecutionStepsFallsBackToTextPatternWhenJsonInvalid() {
        String markdownPlan = """
                ### 第1步：收集信息
                - 使用工具：search
                - 执行方法：先检索资料
                ### 第2步：整理输出
                - 使用工具：none
                - 执行方法：汇总并输出结果
                """;

        Map<Integer, String> steps = parser.parseExecutionSteps(markdownPlan);

        assertThat(steps).hasSize(2);
        assertThat(steps.get(1)).contains("第1步：收集信息");
        assertThat(steps.get(2)).contains("第2步：整理输出");
    }

    @Test
    void extractJsonObjectSupportsPlainJson() {
        String json = "{\"summary\":\"ok\",\"steps\":[{\"step\":1,\"goal\":\"g\",\"tool\":\"t\",\"dependsOn\":[],\"instruction\":\"i\"}]}";

        String extracted = FlowPlanParser.extractJsonObject(json);

        assertThat(extracted).isEqualTo(json);
    }

    @Test
    void escapeTemplateBracesEscapesJsonPrompt() {
        String prompt = "请输出 JSON: {\"summary\":\"整体思路\",\"steps\":[]}";

        String escaped = FlowPlanParser.escapeTemplateBraces(prompt);

        assertThat(escaped).contains("\\{", "\\}");
    }
}
