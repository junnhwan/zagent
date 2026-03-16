package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiAgentFlowConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 智能体流程配置 Mapper, 操作表: ai_agent_flow_config
 */
@Mapper
public interface AiAgentFlowConfigMapper {

    /**
     * 根据智能体ID查询执行流程（按sequence升序）
     *
     * @param agentId 智能体ID
     * @return 按sequence升序排列的流程配置列表
     */
    List<AiAgentFlowConfig> selectByAgentId(@Param("agentId") Long agentId);

}
