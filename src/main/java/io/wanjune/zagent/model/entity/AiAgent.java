package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * AI智能体配置表实体
 * 对应表: ai_agent
 */
@Data
public class AiAgent {
    /** 主键ID */
    private Long id;
    /** 智能体ID（业务唯一标识） */
    private String agentId;
    /** 智能体名称 */
    private String agentName;
    /** 描述 */
    private String description;
    /** 渠道类型 agent(多步编排)/chat_stream(流式对话) */
    private String channel;
    /** 执行策略 fixed(顺序执行)/react(ReAct)/plan_execute(规划执行)/auto(项目自定义多角色协同); 兼容旧值 flow -> plan_execute */
    private String strategy;
    /** 状态 0-禁用 1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
