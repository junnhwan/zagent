package io.wanjune.zagent.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Agent执行结果VO
 *
 * @author zagent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResultVO {

    /** 智能体ID */
    private String agentId;

    /** 智能体名称 */
    private String agentName;

    /** 最终输出（最后一步的结果） */
    private String finalOutput;

    /** 所有执行步骤的详细结果 */
    private List<StepResult> steps;

    /**
     * 单个执行步骤的结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {

        /** 步骤序号 */
        private int sequence;

        /** 该步骤使用的AI客户端ID */
        private String clientId;

        /** 该步骤的输入内容 */
        private String input;

        /** 该步骤的输出内容 */
        private String output;
    }

}
