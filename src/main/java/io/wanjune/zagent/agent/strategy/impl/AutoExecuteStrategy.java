package io.wanjune.zagent.agent.strategy.impl;

import io.wanjune.zagent.agent.strategy.IExecuteStrategy;
import io.wanjune.zagent.model.enums.ClientTypeEnum;
import io.wanjune.zagent.chat.assembly.AiClientAssemblyService;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.Map;

/**
 * Auto策略 - 智能编排（分析→执行→监督→总结）。
 * <p>参考ai-agent-station-study的AutoAgentExecuteStrategy, 实现完整循环状态机:
 * <ol>
 *   <li>分析(TASK_ANALYZER): 深入分析任务, 制定执行策略, 支持提前完成检测</li>
 *   <li>执行(PRECISION_EXECUTOR): 按分析结果精确执行任务</li>
 *   <li>监督(QUALITY_SUPERVISOR): 评估执行质量, PASS/FAIL/OPTIMIZE</li>
 *   <li>如果FAIL/OPTIMIZE, 更新任务描述后回到步骤1（关键反馈循环）</li>
 *   <li>总结(RESPONSE_ASSISTANT): 生成最终报告, 区分完成/未完成</li>
 * </ol></p>
 *
 * @author zagent
 */
@Slf4j
@Service("autoExecuteStrategy")
public class AutoExecuteStrategy implements IExecuteStrategy {

    @Resource
    private AiClientAssemblyService aiClientAssemblyService;

    // ==================== 默认提示词模板（DB未配置时的降级方案） ====================

    private static final String DEFAULT_ANALYZER_PROMPT = """
            **原始用户需求:** %s
            **当前执行步骤:** 第 %d 步 (最大 %d 步)
            **历史执行记录:**
            %s
            **当前任务:** %s
            **分析要求:**
            请深入分析用户的具体需求，制定明确的执行策略：
            1. 理解用户真正想要什么（如：具体的学习计划、项目列表、技术方案等）
            2. 分析需要哪些具体的执行步骤（如：搜索信息、检索项目、生成内容等）
            3. 制定能够产生实际结果的执行策略
            4. 确保策略能够直接回答用户的问题
            **输出格式要求:**
            任务状态分析: [当前任务完成情况的详细分析]
            执行历史评估: [对已完成工作的质量和效果评估]
            下一步策略: [具体的执行计划，包括需要调用的工具和生成的内容]
            完成度评估: [0-100]%%
            任务状态: [CONTINUE/COMPLETED]
            """;

    private static final String DEFAULT_EXECUTOR_PROMPT = """
            **用户原始需求:** %s
            **分析师策略:** %s
            **执行指令:** 你是一个精准任务执行器，需要根据用户需求和分析师策略，实际执行具体的任务。
            **执行要求:**
            1. 直接执行用户的具体需求（如搜索、检索、生成内容等）
            2. 如果需要搜索信息，请实际进行搜索和检索
            3. 如果需要生成计划、列表等，请直接生成完整内容
            4. 提供具体的执行结果，而不只是描述过程
            5. 确保执行结果能直接回答用户的问题
            **输出格式:**
            执行目标: [明确的执行目标]
            执行过程: [实际执行的步骤和调用的工具]
            执行结果: [具体的执行成果和获得的信息/内容]
            质量检查: [对执行结果的质量评估]
            """;

    private static final String DEFAULT_SUPERVISOR_PROMPT = """
            **用户原始需求:** %s
            **执行结果:** %s
            **监督要求:**
            请严格评估执行结果是否真正满足了用户的原始需求：
            1. 检查是否直接回答了用户的问题
            2. 评估内容的完整性和实用性
            3. 确认是否提供了用户期望的具体结果（如学习计划、项目列表等）
            4. 判断是否只是描述过程而没有给出实际答案
            **输出格式（必须严格遵守）:**
            你必须只输出一个合法 JSON 对象，禁止输出解释文字、Markdown、代码块标记。
            JSON 结构如下：
            {
              "decision": "PASS/FAIL/OPTIMIZE",
              "score": 1,
              "match": "需求匹配度分析",
              "issues": "问题识别与不足",
              "improvement": "具体改进建议"
            }
            约束：
            1. decision 只能是 PASS / FAIL / OPTIMIZE
            2. improvement 必须可执行，不能留空
            3. 只输出 JSON，不要输出任何额外说明
            """;

    private static final String DEFAULT_SUMMARY_COMPLETED_PROMPT = """
            基于以下执行过程，请直接回答用户的原始问题，提供最终的答案和结果：
            **用户原始问题:** %s
            **执行历史和过程:**
            %s
            **要求:**
            1. 直接回答用户的原始问题
            2. 基于执行过程中获得的信息和结果
            3. 提供具体、实用的最终答案
            4. 如果是要求制定计划、列表等，请直接给出完整的内容
            5. 避免只描述执行过程，重点是最终答案
            6. 以MD语法的表格形式，优化展示结果数据
            请直接给出用户问题的最终答案：
            """;

    private static final String DEFAULT_SUMMARY_INCOMPLETE_PROMPT = """
            虽然任务未完全执行完成，但请基于已有的执行过程，尽力回答用户的原始问题：
            **用户原始问题:** %s
            **已执行的过程和获得的信息:**
            %s
            **要求:**
            1. 基于已有信息，尽力回答用户的原始问题
            2. 如果信息不足，说明哪些部分无法完成并给出原因
            3. 提供已能确定的部分答案
            4. 给出完成剩余部分的具体建议
            5. 以MD语法的表格形式，优化展示结果数据
            请基于现有信息给出用户问题的答案：
            """;

    @Override
    public String execute(ExecuteContext context, SseEmitter emitter) throws Exception {
        Map<String, String> clientTypeMap = context.getClientTypeMap();
        Map<String, String> stepPromptMap = context.getStepPromptMap();
        int maxStep = context.getMaxStep();
        String userMessage = context.getUserInput();
        String currentTask = userMessage;
        StringBuilder executionHistory = new StringBuilder();
        boolean isCompleted = false;

        String analyzerClientId = clientTypeMap.get(ClientTypeEnum.TASK_ANALYZER.getCode());
        String executorClientId = clientTypeMap.get(ClientTypeEnum.PRECISION_EXECUTOR.getCode());
        String supervisorClientId = clientTypeMap.get(ClientTypeEnum.QUALITY_SUPERVISOR.getCode());
        String summaryClientId = clientTypeMap.get(ClientTypeEnum.RESPONSE_ASSISTANT.getCode());

        // 从DB加载提示词, 无则使用默认值
        String analyzerPrompt = getStepPrompt(stepPromptMap, ClientTypeEnum.TASK_ANALYZER.getCode(), DEFAULT_ANALYZER_PROMPT);
        String executorPrompt = getStepPrompt(stepPromptMap, ClientTypeEnum.PRECISION_EXECUTOR.getCode(), DEFAULT_EXECUTOR_PROMPT);
        String supervisorPrompt = getStepPrompt(stepPromptMap, ClientTypeEnum.QUALITY_SUPERVISOR.getCode(), DEFAULT_SUPERVISOR_PROMPT);
        String[] summaryPrompts = getSummaryPrompts(stepPromptMap);

        for (int step = 1; step <= maxStep; step++) {
            log.info("Auto策略 - 第{}轮开始, 任务: {}", step, currentTask.substring(0, Math.min(100, currentTask.length())));

            // === 阶段1: 分析 ===
            String analysisResult = "";
            if (analyzerClientId != null) {
                ChatClient analyzerClient = aiClientAssemblyService.getOrBuildChatClient(analyzerClientId);
                String historyText = executionHistory.isEmpty() ? "[首次执行]" : executionHistory.toString();
                String prompt = String.format(analyzerPrompt, userMessage, step, maxStep, historyText, currentTask);
                sendStageEvent(emitter, "analysis", "active", step, maxStep, null, context.getConversationId());
                analysisResult = callClient(analyzerClient, prompt, context.getConversationId());
                sendStageEvent(emitter, "analysis", "done", step, maxStep, analysisResult, context.getConversationId());
                log.info("Auto策略 - 第{}轮分析完成", step);

                // 检测提前完成
                if (analysisResult.contains("任务状态: COMPLETED") || analysisResult.contains("完成度评估: 100%")) {
                    log.info("Auto策略 - 分析阶段检测到任务已完成, 跳转总结");
                    isCompleted = true;
                    break;
                }
            }

            // === 阶段2: 执行 ===
            String executionResult = "";
            if (executorClientId != null) {
                ChatClient executorClient = aiClientAssemblyService.getOrBuildChatClient(executorClientId);
                String prompt = String.format(executorPrompt, userMessage, analysisResult);
                sendStageEvent(emitter, "execution", "active", step, maxStep, null, context.getConversationId());
                executionResult = callClient(executorClient, prompt, context.getConversationId());
                sendStageEvent(emitter, "execution", "done", step, maxStep, executionResult, context.getConversationId());

                // 追加执行历史
                executionHistory.append(String.format(
                        "=== 第 %d 步执行记录 ===\n【分析阶段】%s\n【执行阶段】%s\n\n",
                        step, analysisResult, executionResult));
                log.info("Auto策略 - 第{}轮执行完成", step);
            }

            // === 阶段3: 监督 ===
            if (supervisorClientId != null) {
                ChatClient supervisorClient = aiClientAssemblyService.getOrBuildChatClient(supervisorClientId);
                String prompt = String.format(supervisorPrompt, userMessage, executionResult);
                sendStageEvent(emitter, "supervision", "active", step, maxStep, null, context.getConversationId());
                String supervisionResult = callClient(supervisorClient, prompt, context.getConversationId());
                sendStageEvent(emitter, "supervision", "done", step, maxStep, supervisionResult, context.getConversationId());
                log.info("Auto策略 - 第{}轮监督完成", step);

                // 判断质量并更新任务（关键反馈循环）
                log.info("Auto策略 - 监督原文预览: {}", abbreviateForLog(supervisionResult));
                SupervisionDecision decision = parseSupervisionDecision(supervisionResult);
                log.info("Auto策略 - 监督解析方式: {}, decision={}, improvement={}",
                        decision.source(), decision.decision(), abbreviateForLog(decision.improvement()));

                if ("PASS".equals(decision.decision())) {
                    log.info("Auto策略 - 第{}轮质量通过", step);
                    isCompleted = true;
                    break;
                } else if ("FAIL".equals(decision.decision())) {
                    currentTask = "根据质量监督的建议重新执行任务。改进建议: " + decision.improvement();
                    log.info("Auto策略 - 第{}轮质量未通过(FAIL), 更新任务后重新分析", step);
                } else if ("OPTIMIZE".equals(decision.decision())) {
                    currentTask = "根据质量监督的建议优化执行结果。改进建议: " + decision.improvement();
                    log.info("Auto策略 - 第{}轮需优化(OPTIMIZE), 更新任务后重新分析", step);
                }
            } else {
                // 没有监督客户端, 执行一轮即结束
                isCompleted = true;
                break;
            }
        }

        // === 阶段4: 总结 ===
        sendStageEvent(emitter, "summary", "active", 0, maxStep, null, context.getConversationId());
        String finalOutput;
        if (summaryClientId != null) {
            ChatClient summaryClient = aiClientAssemblyService.getOrBuildChatClient(summaryClientId);
            String prompt = isCompleted
                    ? String.format(summaryPrompts[0], userMessage, executionHistory)
                    : String.format(summaryPrompts[1], userMessage, executionHistory);
            finalOutput = callClient(summaryClient, prompt, context.getConversationId());
        } else {
            finalOutput = executionHistory.toString();
        }

        sendStageEvent(emitter, "summary", "done", 0, maxStep, finalOutput, context.getConversationId());
        sendStageEvent(emitter, "complete", "done", 0, maxStep, "执行完成", context.getConversationId());
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
     * 获取总结阶段的两个提示词变体 [completed, incomplete]
     * DB支持JSON格式: {"completed": "...", "incomplete": "..."}
     * 或纯文本格式（仅作为completed使用, incomplete用默认值）
     */
    private String[] getSummaryPrompts(Map<String, String> stepPromptMap) {
        if (stepPromptMap == null) {
            return new String[]{DEFAULT_SUMMARY_COMPLETED_PROMPT, DEFAULT_SUMMARY_INCOMPLETE_PROMPT};
        }
        String dbPrompt = stepPromptMap.get(ClientTypeEnum.RESPONSE_ASSISTANT.getCode());
        if (dbPrompt == null || dbPrompt.isBlank()) {
            return new String[]{DEFAULT_SUMMARY_COMPLETED_PROMPT, DEFAULT_SUMMARY_INCOMPLETE_PROMPT};
        }
        // 尝试JSON解析
        String trimmed = dbPrompt.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = JSONObject.parseObject(trimmed);
                String completed = json.getString("completed");
                String incomplete = json.getString("incomplete");
                return new String[]{
                        (completed != null && !completed.isBlank()) ? completed : DEFAULT_SUMMARY_COMPLETED_PROMPT,
                        (incomplete != null && !incomplete.isBlank()) ? incomplete : DEFAULT_SUMMARY_INCOMPLETE_PROMPT
                };
            } catch (Exception e) {
                log.warn("解析response_assistant的step_prompt JSON失败, 使用默认值", e);
            }
        }
        // 纯文本: 仅作为completed prompt使用
        return new String[]{dbPrompt, DEFAULT_SUMMARY_INCOMPLETE_PROMPT};
    }

    /**
     * 从AI输出中提取指定段落内容
     */
    private String extractSection(String text, String sectionHeader) {
        int idx = text.indexOf(sectionHeader);
        if (idx < 0) return "";
        String after = text.substring(idx + sectionHeader.length()).trim();
        // 截取到下一个段落头或结尾
        int nextSection = after.indexOf("\n");
        for (String header : new String[]{"需求匹配度:", "内容完整性:", "问题识别:", "质量评分:", "是否通过:"}) {
            int pos = after.indexOf(header);
            if (pos > 0 && (nextSection < 0 || pos < nextSection)) {
                nextSection = pos;
            }
        }
        return nextSection > 0 ? after.substring(0, nextSection).trim() : after.trim();
    }

    public static SupervisionDecision parseSupervisionDecision(String supervisionResult) {
        SupervisionDecision jsonDecision = parseSupervisionDecisionFromJson(supervisionResult);
        if (jsonDecision != null) {
            return jsonDecision;
        }

        String improvement = extractSectionStatic(supervisionResult, "改进建议:");
        if (supervisionResult != null) {
            if (supervisionResult.contains("是否通过: PASS") || supervisionResult.contains("PASS")) {
                return new SupervisionDecision("PASS", improvement, "text-fallback");
            }
            if (supervisionResult.contains("是否通过: FAIL") || supervisionResult.contains("FAIL")) {
                return new SupervisionDecision("FAIL", improvement, "text-fallback");
            }
            if (supervisionResult.contains("OPTIMIZE")) {
                return new SupervisionDecision("OPTIMIZE", improvement, "text-fallback");
            }
        }
        return new SupervisionDecision("PASS", improvement, "text-fallback-default");
    }

    private static SupervisionDecision parseSupervisionDecisionFromJson(String supervisionResult) {
        if (supervisionResult == null || supervisionResult.isBlank()) {
            return null;
        }
        try {
            JSONObject json = JSONObject.parseObject(extractJsonObject(supervisionResult));
            if (json == null) {
                return null;
            }
            String decision = trimToEmpty(json.getString("decision")).toUpperCase();
            String improvement = trimToEmpty(json.getString("improvement"));
            if (!("PASS".equals(decision) || "FAIL".equals(decision) || "OPTIMIZE".equals(decision))) {
                throw new IllegalArgumentException("decision 非法: " + decision);
            }
            if (improvement.isBlank()) {
                throw new IllegalArgumentException("improvement 不能为空");
            }
            return new SupervisionDecision(decision, improvement, "json");
        } catch (Exception exception) {
            log.warn("Auto监督JSON解析失败: {}", exception.getMessage());
            return null;
        }
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

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String abbreviateForLog(String text) {
        if (text == null) {
            return "<null>";
        }
        String normalized = text.replaceAll("\s+", " ").trim();
        return normalized.length() > 200 ? normalized.substring(0, 200) + "...(截断)" : normalized;
    }

    private static String extractSectionStatic(String text, String sectionHeader) {
        if (text == null) return "";
        int idx = text.indexOf(sectionHeader);
        if (idx < 0) return "";
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

    public record SupervisionDecision(String decision, String improvement, String source) {}

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
        if (emitter == null) return;
        try {
            StageEvent event = StageEvent.builder()
                    .stage(stage).status(status).step(step).totalSteps(totalSteps)
                    .content(content).sessionId(sessionId).build();
            emitter.send(SseEmitter.event().data(com.alibaba.fastjson.JSON.toJSONString(event)));
        } catch (Exception e) {
            log.error("发送SSE事件失败", e);
        }
    }

}
