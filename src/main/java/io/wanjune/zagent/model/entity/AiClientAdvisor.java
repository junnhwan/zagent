package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * Spring AI Advisor配置
 * 对应表: ai_client_advisor
 * 支持ChatMemory(对话记忆)和RagAnswer(RAG检索增强)
 */
@Data
public class AiClientAdvisor {
    /** 主键ID */
    private Long id;
    /** 顾问ID */
    private String advisorId;
    /** 顾问名称 */
    private String advisorName;
    /** 顾问类型 ChatMemory/RagAnswer */
    private String advisorType;
    /** 顺序号 */
    private Integer orderNum;
    /** 扩展参数(JSON) */
    private String extParam;
    /** 状态 0-禁用 1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
