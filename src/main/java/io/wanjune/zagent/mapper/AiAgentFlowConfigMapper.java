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

    List<AiAgentFlowConfig> selectByAgentId(@Param("agentId") String agentId);

    List<AiAgentFlowConfig> selectAll();

    int insert(AiAgentFlowConfig config);

    int update(AiAgentFlowConfig config);

    int deleteById(@Param("id") Long id);

    int deleteByAgentId(@Param("agentId") String agentId);

}
