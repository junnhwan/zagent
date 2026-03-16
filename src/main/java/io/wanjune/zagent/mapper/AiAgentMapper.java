package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiAgent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * AI智能体 Mapper, 操作表: ai_agent
 */
@Mapper
public interface AiAgentMapper {

    /**
     * 根据智能体ID查询（仅启用状态）
     *
     * @param agentId 智能体ID
     * @return 启用状态的智能体信息
     */
    AiAgent selectByAgentId(@Param("agentId") String agentId);

    /**
     * 查询所有启用的智能体
     *
     * @return 所有启用状态的智能体列表
     */
    List<AiAgent> selectAll();

}
