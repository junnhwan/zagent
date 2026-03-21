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

    AiAgent selectByAgentId(@Param("agentId") String agentId);

    List<AiAgent> selectAll();

    /** 查询所有智能体（包括禁用的，管理用） */
    List<AiAgent> selectAllIncludeDisabled();

    int insert(AiAgent agent);

    int update(AiAgent agent);

    int deleteByAgentId(@Param("agentId") String agentId);

}
