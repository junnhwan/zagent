package io.wanjune.zagent.agent.strategy.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlowPlanParser {

    private static final Pattern DETAIL_STEP_PATTERN = Pattern.compile(
            "###\\s*第(\\d+)步[：:]?\\s*(.*?)\\n((?:(?!###\\s*第\\d+步).|\\n)*)", Pattern.MULTILINE);
    private static final Pattern SIMPLE_STEP_PATTERN = Pattern.compile(
            "第(\\d+)步[：:]?\\s*(.*?)(?=\\n第\\d+步|$)", Pattern.DOTALL);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("第(\\d+)步");

    public Map<Integer, String> parseExecutionSteps(String planText) {
        Map<Integer, String> steps = parseExecutionStepsFromJson(planText);
        if (!steps.isEmpty()) {
            return steps;
        }
        return parseExecutionStepsFromText(planText);
    }

    public static Map<Integer, String> limitExecutionSteps(Map<Integer, String> sourceSteps, int maxStep) {
        if (sourceSteps == null || sourceSteps.isEmpty() || maxStep <= 0) {
            return Collections.emptyMap();
        }
        Map<Integer, String> limited = new LinkedHashMap<>();
        List<Integer> stepNumbers = new ArrayList<>(sourceSteps.keySet());
        Collections.sort(stepNumbers);
        for (Integer stepNumber : stepNumbers) {
            if (limited.size() >= maxStep) {
                break;
            }
            limited.put(stepNumber, sourceSteps.get(stepNumber));
        }
        return limited;
    }

    private Map<Integer, String> parseExecutionStepsFromJson(String planText) {
        Map<Integer, String> stepsMap = new LinkedHashMap<>();
        try {
            String jsonText = extractJsonObject(planText);
            if (jsonText == null) {
                return stepsMap;
            }
            JSONObject root = JSON.parseObject(jsonText);
            JSONArray steps = root.getJSONArray("steps");
            if (steps == null || steps.isEmpty()) {
                return stepsMap;
            }

            for (int index = 0; index < steps.size(); index++) {
                JSONObject step = steps.getJSONObject(index);
                if (step == null) {
                    continue;
                }
                Integer stepNumber = step.getInteger("step");
                if (stepNumber == null) {
                    stepNumber = index + 1;
                }
                String goal = trimToEmpty(step.getString("goal"));
                String tool = trimToEmpty(step.getString("tool"));
                String instruction = trimToEmpty(step.getString("instruction"));
                JSONArray dependsOn = step.getJSONArray("dependsOn");
                String dependsText = dependsOn == null || dependsOn.isEmpty() ? "[]" : dependsOn.toJSONString();

                StringBuilder stepText = new StringBuilder();
                stepText.append("第").append(stepNumber).append("步- ").append(goal);
                if (!tool.isBlank()) {
                    stepText.append("\n使用工具：").append(tool);
                }
                stepText.append("\n依赖步骤：").append(dependsText);
                if (!instruction.isBlank()) {
                    stepText.append("\n执行说明：").append(instruction);
                }
                stepsMap.put(stepNumber, stepText.toString());
            }
        } catch (Exception ignored) {
        }
        return stepsMap;
    }

    private Map<Integer, String> parseExecutionStepsFromText(String planText) {
        Map<Integer, String> stepsMap = new LinkedHashMap<>();
        if (planText == null || planText.isBlank()) {
            return stepsMap;
        }

        Matcher detailMatcher = DETAIL_STEP_PATTERN.matcher(planText);
        while (detailMatcher.find()) {
            int stepNum = Integer.parseInt(detailMatcher.group(1));
            String title = trimToEmpty(detailMatcher.group(2));
            String body = trimToEmpty(detailMatcher.group(3));
            stepsMap.put(stepNum, ("第" + stepNum + "步：" + title + "\n" + body).trim());
        }
        if (!stepsMap.isEmpty()) {
            return stepsMap;
        }

        Matcher simpleMatcher = SIMPLE_STEP_PATTERN.matcher(planText);
        while (simpleMatcher.find()) {
            int stepNum = Integer.parseInt(simpleMatcher.group(1));
            stepsMap.put(stepNum, ("第" + stepNum + "步：" + trimToEmpty(simpleMatcher.group(2))).trim());
        }
        if (!stepsMap.isEmpty()) {
            return stepsMap;
        }

        Matcher numberMatcher = NUMBER_PATTERN.matcher(planText);
        int fallbackStep = 1;
        while (numberMatcher.find()) {
            stepsMap.putIfAbsent(fallbackStep++, numberMatcher.group());
        }
        return stepsMap;
    }

    public static String extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```") && trimmed.contains("{")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            return start >= 0 && end > start ? trimmed.substring(start, end + 1) : null;
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        return start >= 0 && end > start ? trimmed.substring(start, end + 1) : null;
    }

    public static String escapeTemplateBraces(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }
        return prompt.replace("{", "\\{").replace("}", "\\}");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
