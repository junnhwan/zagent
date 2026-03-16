package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * AI客户端统一关联配置表
 * 对应表: ai_client_config
 * 核心关系图表: 通过source->target将Client/Model与API/Prompt/Advisor/MCP关联
 */
@Data
public class AiClientConfig {
    /** 主键ID */
    private Long id;
    /** 源类型 model/client */
    private String sourceType;
    /** 源ID */
    private String sourceId;
    /** 目标类型 api/model/prompt/advisor/tool_mcp */
    private String targetType;
    /** 目标ID */
    private String targetId;
    /** 扩展参数(JSON) */
    private String extParam;
    /** 状态 0-禁用 1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
