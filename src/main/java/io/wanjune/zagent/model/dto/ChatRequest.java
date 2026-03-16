package io.wanjune.zagent.model.dto;

import lombok.Data;

/**
 * 对话请求DTO
 *
 * @author zagent
 */
@Data
public class ChatRequest {

    /** 要使用的AI客户端ID（对应ai_client表的client_id） */
    private String clientId;

    /** 用户消息内容 */
    private String message;

    /** 对话ID, 用于对话记忆的连续性, 为空则自动生成 */
    private String conversationId;

}
