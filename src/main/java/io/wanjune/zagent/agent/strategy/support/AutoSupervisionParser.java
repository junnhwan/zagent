package io.wanjune.zagent.agent.strategy.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.wanjune.zagent.agent.strategy.impl.AutoExecuteStrategy;

public class AutoSupervisionParser {

    public AutoExecuteStrategy.SupervisionDecision parseSupervisionDecision(String supervisionResult) {
        AutoExecuteStrategy.SupervisionDecision fromJson = parseSupervisionDecisionFromJson(supervisionResult);
        if (fromJson != null) {
            return fromJson;
        }

        String improvement = extractSection(supervisionResult, "改进建议:");
        if (supervisionResult != null) {
            if (supervisionResult.contains("是否通过: PASS") || supervisionResult.contains("PASS")) {
                return new AutoExecuteStrategy.SupervisionDecision("PASS", improvement, "text-fallback");
            }
            if (supervisionResult.contains("是否通过: FAIL") || supervisionResult.contains("FAIL")) {
                return new AutoExecuteStrategy.SupervisionDecision("FAIL", improvement, "text-fallback");
            }
            if (supervisionResult.contains("OPTIMIZE")) {
                return new AutoExecuteStrategy.SupervisionDecision("OPTIMIZE", improvement, "text-fallback");
            }
        }
        return new AutoExecuteStrategy.SupervisionDecision("PASS", improvement, "text-fallback-default");
    }

    private AutoExecuteStrategy.SupervisionDecision parseSupervisionDecisionFromJson(String supervisionResult) {
        try {
            JSONObject json = JSON.parseObject(FlowPlanParser.extractJsonObject(supervisionResult));
            if (json == null) {
                return null;
            }
            String decision = trimToEmpty(json.getString("decision")).toUpperCase();
            String improvement = trimToEmpty(json.getString("improvement"));
            if (!("PASS".equals(decision) || "FAIL".equals(decision) || "OPTIMIZE".equals(decision))) {
                return null;
            }
            if (improvement.isBlank()) {
                return null;
            }
            return new AutoExecuteStrategy.SupervisionDecision(decision, improvement, "json");
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractSection(String text, String sectionHeader) {
        if (text == null || text.isBlank() || sectionHeader == null || sectionHeader.isBlank()) {
            return "";
        }
        int idx = text.indexOf(sectionHeader);
        if (idx < 0) {
            return "";
        }
        String after = text.substring(idx + sectionHeader.length()).trim();
        int nextSection = after.indexOf("\n");
        for (String header : new String[]{"需求匹配度:", "内容完整性:", "问题识别:", "质量评分:", "是否通过:"}) {
            int pos = after.indexOf(header);
            if (pos > 0 && (nextSection < 0 || pos < nextSection)) {
                nextSection = pos;
            }
        }
        return nextSection > 0 ? after.substring(0, nextSection).trim() : after.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
