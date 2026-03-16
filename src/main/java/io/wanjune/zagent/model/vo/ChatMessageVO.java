package io.wanjune.zagent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话消息响应VO
 *
 * @author zagent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    /** 角色: assistant */
    private String role;

    /** AI回复内容 */
    private String content;

    /** 对话ID, 可用于后续对话保持记忆 */
    private String conversationId;

}
