package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * 智能体定时任务调度配置
 * 对应表: ai_agent_task_schedule
 */
@Data
public class AiAgentTaskSchedule {
    /** 主键ID */
    private Long id;
    /** 智能体ID */
    private Long agentId;
    /** 任务名称 */
    private String taskName;
    /** 任务描述 */
    private String description;
    /** Cron表达式 */
    private String cronExpression;
    /** 任务入参配置(JSON) */
    private String taskParam;
    /** 状态 0-无效 1-有效 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
