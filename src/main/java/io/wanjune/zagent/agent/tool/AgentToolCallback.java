package io.wanjune.zagent.agent.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.wanjune.zagent.agent.service.AgentService;
import io.wanjune.zagent.model.dto.AgentRunRequest;
import io.wanjune.zagent.model.vo.AgentResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * 将Agent包装为Spring AI ToolCallback, 使其可被其他Agent作为工具调用。
 * <p>当主Agent（如ReAct策略）需要调用子Agent时, 通过此类将子Agent暴露为标准工具接口。
 * LLM将以 {"input": "具体任务"} 格式传入参数, 本类解析后委托AgentService执行。</p>
 *
 * @author zagent
 */
@Slf4j
public class AgentToolCallback implements ToolCallback {

    private final String agentId;
    private final String agentName;
    private final String description;
    private final AgentService agentService;

    /** Agent-as-Tool调用时的默认最大执行轮次 */
    private static final int DEFAULT_MAX_STEP = 3;

    private static final String INPUT_SCHEMA = """
            {
                "type": "object",
                "properties": {
                    "input": {
                        "type": "string",
                        "description": "The task or question to send to this agent"
                    }
                },
                "required": ["input"]
            }
            """;

    public AgentToolCallback(String agentId, String agentName, String description, AgentService agentService) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.description = description;
        this.agentService = agentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        // Tool name: agent_<agentId>, 替换非法字符确保函数调用安全
        String toolName = "agent_" + agentId.replaceAll("[^a-zA-Z0-9_]", "_");
        return ToolDefinition.builder()
                .name(toolName)
                .description(description != null && !description.isBlank()
                        ? description
                        : "Sub-agent: " + agentName)
                .inputSchema(INPUT_SCHEMA)
                .build();
    }

    @Override
    public String call(String input) {
        return call(input, null);
    }

    @Override
    public String call(String input, ToolContext toolContext) {
        // 递归保护: 检查当前agentId是否已在调用栈中
        if (AgentToolRegistry.isInCallStack(agentId)) {
            log.warn("Agent-as-Tool recursion detected: agentId={}, 拒绝执行以防止无限递归", agentId);
            return "Recursive call detected: agent '" + agentName + "' is already in the call chain. Skipping to prevent infinite loop.";
        }

        AgentToolRegistry.enterCallStack(agentId);
        try {
            String taskInput = extractTaskInput(input);

            log.info("Agent-as-Tool invocation: agentId={}, agentName={}, inputPreview={}",
                    agentId, agentName, abbreviate(taskInput, 200));

            AgentRunRequest request = new AgentRunRequest();
            request.setAgentId(agentId);
            request.setInput(taskInput);
            request.setMaxStep(DEFAULT_MAX_STEP);

            AgentResultVO result = agentService.run(request);
            String output = result.getFinalOutput();

            log.info("Agent-as-Tool completed: agentId={}, outputPreview={}",
                    agentId, abbreviate(output, 200));

            return output != null ? output : "Agent returned no output.";
        } catch (Exception e) {
            log.error("Agent-as-Tool failed: agentId={}", agentId, e);
            return "Agent execution failed: " + e.getMessage();
        } finally {
            AgentToolRegistry.leaveCallStack(agentId);
        }
    }

    /**
     * 从LLM传入的JSON参数中提取实际任务输入。
     * LLM通常以 {"input": "..."} 格式调用, 但也可能直接传入纯文本。
     */
    private String extractTaskInput(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        try {
            JSONObject json = JSON.parseObject(input);
            String taskInput = json.getString("input");
            if (taskInput != null && !taskInput.isBlank()) {
                return taskInput;
            }
        } catch (Exception ignored) {
            // 非JSON格式, 直接使用原始输入
        }
        return input;
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null) return "<null>";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }
}
