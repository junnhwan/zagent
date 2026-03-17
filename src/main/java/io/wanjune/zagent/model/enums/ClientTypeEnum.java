package io.wanjune.zagent.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户端类型枚举, 标识FlowConfig中每个客户端的角色
 *
 * @author zagent
 */
@Getter
@AllArgsConstructor
public enum ClientTypeEnum {

    /** 默认/通用客户端 */
    DEFAULT("default"),
    /** Auto策略 - 任务分析器 */
    TASK_ANALYZER("task_analyzer"),
    /** Auto策略 - 精确执行器 */
    PRECISION_EXECUTOR("precision_executor"),
    /** Auto策略 - 质量监督员 */
    QUALITY_SUPERVISOR("quality_supervisor"),
    /** Auto策略 - 响应助手/总结 */
    RESPONSE_ASSISTANT("response_assistant"),
    /** Flow策略 - MCP工具客户端 */
    TOOL_MCP("tool_mcp"),
    /** Flow策略 - 规划客户端 */
    PLANNING("planning"),
    /** Flow策略 - 执行客户端 */
    EXECUTOR("executor");

    private final String code;

    public static ClientTypeEnum of(String code) {
        for (ClientTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return DEFAULT;
    }

}
