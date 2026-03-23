package io.wanjune.zagent.agent.strategy.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.wanjune.zagent.agent.strategy.IExecuteStrategy;
import io.wanjune.zagent.model.enums.ClientTypeEnum;
import io.wanjune.zagent.chat.assembly.AiClientAssemblyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flow策略 - MCP工具驱动编排（工具分析→规划→解析→执行）。
 * <p>参考ai-agent-station-study的FlowAgentExecuteStrategy, 完整实现:
 * <ol>
 *   <li>工具分析(TOOL_MCP): 详细分析可用MCP工具的能力和适用场景</li>
 *   <li>规划(PLANNING): 基于工具能力制定结构化执行计划</li>
 *   <li>解析: 将计划解析为可执行的步骤列表（纯Java，无LLM调用）</li>
 *   <li>逐步执行(EXECUTOR): 按计划执行每个步骤，带错误处理和限流</li>
 * </ol></p>
 *
 * @author zagent
 */
@Slf4j
@Service("flowExecuteStrategy")
public class FlowExecuteStrategy implements IExecuteStrategy {

    @Resource
    private AiClientAssemblyService aiClientAssemblyService;

    // ==================== 默认提示词模板（DB未配置时的降级方案） ====================

    private static final String DEFAULT_MCP_ANALYSIS_PROMPT = """
            # MCP工具能力分析任务

            ## 重要说明
            **注意：本阶段仅进行MCP工具能力分析，不执行用户的实际请求。**
            这是一个纯分析阶段，目的是评估可用工具的能力和适用性，为后续的执行规划提供依据。

            ## 用户请求
            %s

            ## 分析要求
            请基于当前可用的MCP工具信息，针对用户请求进行详细的工具能力分析（仅分析，不执行）：

            ### 1. 工具匹配分析
            - 分析每个可用工具的核心功能和适用场景
            - 评估哪些工具能够满足用户请求的具体需求
            - 标注每个工具的匹配度（高/中/低）

            ### 2. 工具使用指南
            - 提供每个相关工具的具体调用方式
            - 说明必需的参数和可选参数
            - 给出参数的示例值和格式要求

            ### 3. 执行策略建议
            - 推荐最优的工具组合方案
            - 建议工具的调用顺序和依赖关系
            - 提供备选方案和降级策略

            ### 4. 注意事项
            - 标注工具的使用限制和约束条件
            - 提醒可能的错误情况和处理方式
            - 给出性能优化建议

            ### 5. 分析总结
            - 明确说明这是分析阶段，不要执行任何实际操作
            - 总结工具能力评估结果
            - 为后续执行阶段提供建议

            请确保分析结果准确、详细、可操作，并再次强调这仅是分析阶段。
            """;

    private static final String DEFAULT_PLANNING_PROMPT = """
            # 智能执行计划生成

            ## 用户需求分析
            **完整用户请求：**
            %s

            **重要提醒：** 在生成执行计划时，必须完整保留和传递用户请求中的所有详细信息，包括但不限于：
            - 任务的具体目标和期望结果
            - 涉及的数据、参数、配置等详细信息
            - 特定的业务规则、约束条件或要求
            - 输出格式、质量标准或验收条件

            ## MCP工具能力分析结果
            %s

            ## 执行计划要求
            请基于上述用户详细需求和MCP工具分析结果，生成精确的执行计划：

            ### 核心要求
            1. **完整保留用户需求**: 必须将用户请求中的所有详细信息完整传递到每个执行步骤中
            2. **严格遵循MCP分析结果**: 必须根据工具能力分析中的匹配度和推荐方案制定步骤
            3. **精确工具映射**: 每个步骤必须使用确切的函数名称
            4. **参数完整性**: 所有工具调用必须包含用户原始需求中的完整参数信息
            5. **依赖关系明确**: 合理安排步骤顺序，确保前置条件得到满足
            6. **合理粒度**: 避免过度细分，每个步骤应该是完整且独立的功能单元

            ### 输出格式（必须严格遵守）
            你必须只输出一个合法 JSON 对象，禁止输出 Markdown、解释文字、代码块标记。

            JSON 结构如下：
            {
              "summary": "一句话概括整体执行思路",
              "steps": [
                {
                  "step": 1,
                  "goal": "本步骤要完成的目标",
                  "tool": "必须填写确切的工具或函数名称；若无需工具则填写 none",
                  "dependsOn": [],
                  "instruction": "给执行阶段的完整指令，必须完整保留用户请求中的关键细节、参数、约束和输出要求"
                }
              ]
            }

            ### 质量检查清单
            生成计划后请确认：
            - 输出是合法 JSON，而不是 Markdown
            - steps 数组不为空
            - 每个 step 都有 step、goal、tool、dependsOn、instruction
            - step 从 1 开始递增
            - instruction 具体、可执行、保留用户关键细节

            现在请直接输出执行计划 JSON：
            """;

    private static final String DEFAULT_STEP_EXECUTION_PROMPT = """
            你是一个智能执行助手，需要执行以下步骤:

            **步骤内容:**
            %s

            **用户原始请求:**
            %s

            %s

            **执行要求:**
            1. 仔细分析步骤内容，理解需要执行的具体任务
            2. 如果涉及MCP工具调用，请使用相应的工具
            3. 提供详细的执行过程和结果
            4. 如果遇到问题，请说明具体的错误信息
            5. 执行完成后，必须在回复末尾明确输出执行结果，格式如下:
               === 执行结果 ===
               状态: [成功/失败]
               结果描述: [具体的执行结果描述]

            请开始执行这个步骤，并严格按照要求提供详细的执行报告和结果输出。
            """;

    // 主模式: ### 第N步：...
    private static final Pattern DETAIL_STEP_PATTERN = Pattern.compile(
            "###\\s*(第\\d+步[：:][^\\n]+)([\\s\\S]*?)(?=###\\s*第\\d+步|$)");
    // 备选模式: [ ] 第N步：...
    private static final Pattern SIMPLE_STEP_PATTERN = Pattern.compile(
            "\\[\\s*]\\s*(第\\d+步[：:][^\\n]+)");
    // 提取步骤编号
    private static final Pattern NUMBER_PATTERN = Pattern.compile("第(\\d+)步");

    @Override
    public String execute(ExecuteContext context, SseEmitter emitter) throws Exception {
        Map<String, String> clientTypeMap = context.getClientTypeMap();
        Map<String, String> stepPromptMap = context.getStepPromptMap();
        String userInput = context.getUserInput();

        // 从DB加载提示词, 无则使用默认值
        String mcpAnalysisPrompt = getStepPrompt(stepPromptMap, ClientTypeEnum.TOOL_MCP.getCode(), DEFAULT_MCP_ANALYSIS_PROMPT);
        String planningPrompt = getStepPrompt(stepPromptMap, ClientTypeEnum.PLANNING.getCode(), DEFAULT_PLANNING_PROMPT);
        String stepExecutionPrompt = getStepPrompt(stepPromptMap, ClientTypeEnum.EXECUTOR.getCode(), DEFAULT_STEP_EXECUTION_PROMPT);

        // === 阶段1: MCP工具分析 ===
        String toolAnalysis = "";
        String mcpClientId = clientTypeMap.get(ClientTypeEnum.TOOL_MCP.getCode());
        if (mcpClientId != null) {
            log.info("Flow策略 - 开始MCP工具分析");
            ChatClient mcpClient = aiClientAssemblyService.getOrBuildChatClient(mcpClientId);
            String prompt = String.format(mcpAnalysisPrompt, userInput);
            sendStageEvent(emitter, "tool_analysis", "active", 1, 0, null, context.getConversationId());
            toolAnalysis = callClient(mcpClient, prompt, context.getConversationId());
            sendStageEvent(emitter, "tool_analysis", "done", 1, 0, toolAnalysis, context.getConversationId());
            log.info("Flow策略 - 工具分析完成");
        }

        // === 阶段2: 制定执行计划 ===
        String planResult = "";
        String planningClientId = clientTypeMap.get(ClientTypeEnum.PLANNING.getCode());
        if (planningClientId != null) {
            log.info("Flow策略 - 开始制定执行计划");
            ChatClient planningClient = aiClientAssemblyService.getOrBuildChatClient(planningClientId);
            String prompt = String.format(planningPrompt, userInput, toolAnalysis)
                    + "\n\n## 步数限制\n最多规划 " + context.getMaxStep() + " 步，严禁输出超过该数量的执行步骤。";
            sendStageEvent(emitter, "planning", "active", 2, 0, null, context.getConversationId());
            planResult = callClient(planningClient, prompt, context.getConversationId());
            log.info("Flow策略 - 规划原文预览: {}", abbreviateForLog(planResult));
            sendStageEvent(emitter, "planning", "done", 2, 0, planResult,
                    buildPlanningPayload(planResult, context.getMaxStep()), context.getConversationId());
            log.info("Flow策略 - 规划完成");
        }

        // === 阶段3: 解析执行计划为步骤列表（纯Java，无LLM调用） ===
        Map<Integer, String> stepsMap = parseExecutionSteps(planResult);
        if (stepsMap.isEmpty()) {
            stepsMap.put(1, planResult.isEmpty() ? userInput : planResult);
        }
        stepsMap = limitExecutionSteps(stepsMap, context.getMaxStep());
        log.info("Flow策略 - 解析出{}个执行步骤", stepsMap.size());

        // === 阶段4: 逐步执行 ===
        String executorClientId = clientTypeMap.get(ClientTypeEnum.EXECUTOR.getCode());
        if (executorClientId == null) {
            sendStageEvent(emitter, "complete", "done", 0, 0, "无执行客户端，返回计划", context.getConversationId());
            return planResult;
        }

        ChatClient executorClient = aiClientAssemblyService.getOrBuildChatClient(executorClientId);
        StringBuilder allResults = new StringBuilder();
        List<Integer> sortedSteps = new ArrayList<>(stepsMap.keySet());
        Collections.sort(sortedSteps);
        int totalExecSteps = sortedSteps.size();

        for (int stepNum : sortedSteps) {
            String stepContent = stepsMap.get(stepNum);
            log.info("Flow策略 - 执行第{}步", stepNum);

            String previousResults = allResults.isEmpty() ? "" : "**前序步骤执行结果:**\n" + allResults;
            String prompt = String.format(stepExecutionPrompt, stepContent, userInput, previousResults);

            sendStageEvent(emitter, "step_execution", "active", stepNum, totalExecSteps, null, context.getConversationId());
            try {
                String stepResult = callClient(executorClient, prompt, context.getConversationId());
                allResults.append(String.format("=== 第%d步结果 ===\n%s\n\n", stepNum, stepResult));
                String sseContent = stepResult.length() > 500
                        ? stepResult.substring(0, 500) + "...(已截断)"
                        : stepResult;
                sendStageEvent(emitter, "step_execution", "done", stepNum, totalExecSteps, sseContent, context.getConversationId());
                log.info("Flow策略 - 第{}步执行成功", stepNum);
            } catch (Exception e) {
                String errorMsg = String.format("第%d步执行失败: %s", stepNum, e.getMessage());
                allResults.append(errorMsg).append("\n\n");
                sendStageEvent(emitter, "step_execution", "error", stepNum, totalExecSteps, errorMsg, context.getConversationId());
                log.error("Flow策略 - 第{}步执行失败", stepNum, e);
            }

            // 步骤间限流
            Thread.sleep(1000);
        }

        String finalOutput = allResults.toString();
        sendStageEvent(emitter, "summary", "done", 0, totalExecSteps, finalOutput, context.getConversationId());
        sendStageEvent(emitter, "complete", "done", 0, totalExecSteps, "执行完成", context.getConversationId());
        return finalOutput;
    }

    /**
     * 从stepPromptMap获取指定角色的提示词, DB未配置时返回默认值
     */
    private String getStepPrompt(Map<String, String> stepPromptMap, String clientType, String defaultPrompt) {
        if (stepPromptMap == null) return defaultPrompt;
        String dbPrompt = stepPromptMap.get(clientType);
        return (dbPrompt != null && !dbPrompt.isBlank()) ? dbPrompt : defaultPrompt;
    }

    /**
     * 限制最终执行步数，避免规划结果无限扩张。
     */
    static Map<Integer, String> limitExecutionSteps(Map<Integer, String> sourceSteps, int maxStep) {
        if (sourceSteps == null || sourceSteps.isEmpty()) {
            return Collections.emptyMap();
        }
        int safeMaxStep = maxStep > 0 ? maxStep : 3;
        Map<Integer, String> limited = new LinkedHashMap<>();
        sourceSteps.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(safeMaxStep)
                .forEach(entry -> limited.put(entry.getKey(), entry.getValue()));
        return limited;
    }

    private Map<Integer, String> parseExecutionSteps(String planText) {
        Map<Integer, String> jsonSteps = parseExecutionStepsFromJson(planText);
        if (!jsonSteps.isEmpty()) {
            log.info("Flow策略 - 计划解析方式: json, steps={}", jsonSteps.size());
            return jsonSteps;
        }

        log.warn("Flow规划结果未通过JSON解析，回退到文本正则解析");
        Map<Integer, String> textSteps = parseExecutionStepsFromText(planText);
        if (!textSteps.isEmpty()) {
            log.info("Flow策略 - 计划解析方式: text-fallback, steps={}", textSteps.size());
        }
        return textSteps;
    }

    private Map<Integer, String> parseExecutionStepsFromJson(String planText) {
        Map<Integer, String> steps = new LinkedHashMap<>();
        if (planText == null || planText.isBlank()) return steps;

        try {
            JSONObject root = JSON.parseObject(extractJsonObject(planText));
            if (root == null) {
                return steps;
            }

            JSONArray stepArray = root.getJSONArray("steps");
            if (stepArray == null || stepArray.isEmpty()) {
                log.warn("Flow规划JSON缺少有效的 steps 数组");
                return Collections.emptyMap();
            }

            Set<Integer> stepNumbers = new HashSet<>();
            for (int index = 0; index < stepArray.size(); index++) {
                JSONObject stepObject = stepArray.getJSONObject(index);
                if (stepObject == null) {
                    throw new IllegalArgumentException("steps[" + index + "] 不是合法对象");
                }

                Integer stepNum = stepObject.getInteger("step");
                String goal = trimToEmpty(stepObject.getString("goal"));
                String tool = trimToEmpty(stepObject.getString("tool"));
                String instruction = trimToEmpty(stepObject.getString("instruction"));
                JSONArray dependsOn = stepObject.getJSONArray("dependsOn");

                if (stepNum == null || stepNum <= 0) {
                    throw new IllegalArgumentException("steps[" + index + "].step 必须为正整数");
                }
                if (!stepNumbers.add(stepNum)) {
                    throw new IllegalArgumentException("steps 中存在重复 step: " + stepNum);
                }
                if (goal.isBlank()) {
                    throw new IllegalArgumentException("steps[" + index + "].goal 不能为空");
                }
                if (tool.isBlank()) {
                    throw new IllegalArgumentException("steps[" + index + "].tool 不能为空");
                }
                if (instruction.isBlank()) {
                    throw new IllegalArgumentException("steps[" + index + "].instruction 不能为空");
                }

                String dependsOnText = dependsOn == null || dependsOn.isEmpty() ? "[]" : dependsOn.toJSONString();
                steps.put(stepNum, String.format(
                        "步骤标题: 第%d步 - %s\n使用工具: %s\n依赖步骤: %s\n执行指令:\n%s",
                        stepNum, goal, tool, dependsOnText, instruction
                ));
            }
            log.info("Flow策略 - JSON规划校验通过: steps={}", steps.keySet());
            return steps;
        } catch (Exception exception) {
            log.warn("Flow规划JSON解析失败: {}", exception.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Integer, String> parseExecutionStepsFromText(String planText) {
        Map<Integer, String> steps = new LinkedHashMap<>();
        if (planText == null || planText.isBlank()) return steps;

        // 第一轮: 匹配 ### 第N步：标题 + 详细内容
        Matcher detailMatcher = DETAIL_STEP_PATTERN.matcher(planText);
        while (detailMatcher.find()) {
            String title = detailMatcher.group(1).trim();
            String content = detailMatcher.group(2).trim();
            Matcher numMatcher = NUMBER_PATTERN.matcher(title);
            if (numMatcher.find()) {
                int stepNum = Integer.parseInt(numMatcher.group(1));
                steps.put(stepNum, title + "\n" + content);
            }
        }

        // 第二轮: 如果第一轮没匹配到，尝试 [ ] 第N步：格式
        if (steps.isEmpty()) {
            Matcher simpleMatcher = SIMPLE_STEP_PATTERN.matcher(planText);
            while (simpleMatcher.find()) {
                String title = simpleMatcher.group(1).trim();
                Matcher numMatcher = NUMBER_PATTERN.matcher(title);
                if (numMatcher.find()) {
                    int stepNum = Integer.parseInt(numMatcher.group(1));
                    steps.put(stepNum, title);
                }
            }
        }

        return steps;
    }

    static String extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            return trimmed;
        }
        return trimmed.substring(start, end + 1);
    }

    private Object buildPlanningPayload(String planText, int maxStep) {
        Map<Integer, String> parsedSteps = limitExecutionSteps(parseExecutionSteps(planText), maxStep);
        if (parsedSteps.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> steps = new ArrayList<>();
        parsedSteps.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> steps.add(Map.of(
                        "step", entry.getKey(),
                        "content", entry.getValue()
                )));
        return Map.of("steps", steps);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String abbreviateForLog(String text) {
        if (text == null) {
            return "<null>";
        }
        String normalized = text.replaceAll("\s+", " ").trim();
        return normalized.length() > 200 ? normalized.substring(0, 200) + "...(截断)" : normalized;
    }

    private String callClient(ChatClient client, String prompt, String conversationId) {
        return client.prompt(prompt)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> a
                        .param("chat_memory_conversation_id", conversationId)
                        .param("chat_memory_response_size", 1024))
                .call()
                .content();
    }

    private void sendStageEvent(SseEmitter emitter, String stage, String status, int step, int totalSteps, String content, String sessionId) {
        sendStageEvent(emitter, stage, status, step, totalSteps, content, null, sessionId);
    }

    private void sendStageEvent(SseEmitter emitter, String stage, String status, int step, int totalSteps,
                                String content, Object payload, String sessionId) {
        if (emitter == null) return;
        try {
            StageEvent event = StageEvent.builder()
                    .stage(stage).status(status).step(step).totalSteps(totalSteps)
                    .content(content).payload(payload).sessionId(sessionId).build();
            emitter.send(SseEmitter.event().data(com.alibaba.fastjson.JSON.toJSONString(event)));
        } catch (Exception e) {
            log.error("发送SSE事件失败", e);
        }
    }

}
