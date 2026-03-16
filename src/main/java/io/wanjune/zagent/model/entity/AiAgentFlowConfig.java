package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * 智能体执行流水线配置
 * 对应表: ai_agent_flow_config
 * 定义Agent按sequence顺序依次调用哪些Client
 */
@Data
public class AiAgentFlowConfig {
    /** 主键ID */
    private Long id;
    /** 智能体ID */
    private Long agentId;
    /** 客户端ID */
    private Long clientId;
    /** 序列号（执行顺序，从小到大） */
    private Integer sequence;
    /** 创建时间 */
    private Date createTime;
}
