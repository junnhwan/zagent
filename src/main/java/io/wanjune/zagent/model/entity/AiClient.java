package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * AI客户端配置表实体
 * 对应表: ai_client
 * 一个Client通过ai_client_config关联表组装API/Model/Prompt/Advisor/MCP
 */
@Data
public class AiClient {
    /** 主键ID */
    private Long id;
    /** 客户端ID（业务唯一标识） */
    private String clientId;
    /** 客户端名称 */
    private String clientName;
    /** 描述 */
    private String description;
    /** 状态 0-禁用 1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
