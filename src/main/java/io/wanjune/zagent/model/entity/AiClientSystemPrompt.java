package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * 系统提示词配置
 * 对应表: ai_client_system_prompt
 * 一个Client可关联多个提示词
 */
@Data
public class AiClientSystemPrompt {
    /** 主键ID */
    private Long id;
    /** 提示词ID */
    private String promptId;
    /** 提示词名称 */
    private String promptName;
    /** 提示词内容 */
    private String promptContent;
    /** 描述 */
    private String description;
    /** 状态 0-禁用 1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
