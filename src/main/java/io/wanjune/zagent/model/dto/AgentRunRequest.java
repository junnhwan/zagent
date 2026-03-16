package io.wanjune.zagent.model.dto;

import lombok.Data;

/**
 * Agent执行请求DTO
 *
 * @author zagent
 */
@Data
public class AgentRunRequest {

    /** 要执行的智能体ID（对应ai_agent表的agent_id） */
    private String agentId;

    /** 用户输入/初始消息 */
    private String input;

    /** 对话ID, 用于对话记忆 */
    private String conversationId;

}
