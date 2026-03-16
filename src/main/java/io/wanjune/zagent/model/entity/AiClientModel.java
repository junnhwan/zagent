package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * 聊天模型配置
 * 对应表: ai_client_model
 * 通过api_id关联到具体API端点
 */
@Data
public class AiClientModel {
    /** 主键ID */
    private Long id;
    /** 模型ID */
    private String modelId;
    /** 关联的API配置ID */
    private String apiId;
    /** 模型名称(如gpt-4.1-mini) */
    private String modelName;
    /** 模型类型 openai/deepseek/claude */
    private String modelType;
    /** 状态 0-禁用 1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
