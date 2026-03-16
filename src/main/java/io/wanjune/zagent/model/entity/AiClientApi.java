package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * OpenAI兼容API端点配置
 * 对应表: ai_client_api
 * 存储API地址、密钥和各接口路径
 */
@Data
public class AiClientApi {
    /** 主键ID */
    private Long id;
    /** API配置ID */
    private String apiId;
    /** API基础URL */
    private String baseUrl;
    /** API密钥 */
    private String apiKey;
    /** 补全接口路径 */
    private String completionsPath;
    /** 嵌入接口路径 */
    private String embeddingsPath;
    /** 状态 0-禁用 1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
