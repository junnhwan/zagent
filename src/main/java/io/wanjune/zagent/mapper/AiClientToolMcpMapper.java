package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiClientToolMcp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * MCP工具配置 Mapper, 操作表: ai_client_tool_mcp
 */
@Mapper
public interface AiClientToolMcpMapper {

    /**
     * 根据MCP工具ID查询
     *
     * @param mcpId MCP工具ID
     * @return MCP工具配置信息
     */
    AiClientToolMcp selectByMcpId(@Param("mcpId") String mcpId);

    /**
     * 根据MCP工具ID列表批量查询
     *
     * @param mcpIds MCP工具ID列表
     * @return MCP工具配置列表
     */
    List<AiClientToolMcp> selectByMcpIds(@Param("mcpIds") List<String> mcpIds);

    List<AiClientToolMcp> selectAll();

    int insert(AiClientToolMcp mcp);

    int update(AiClientToolMcp mcp);

    int deleteByMcpId(@Param("mcpId") String mcpId);

}
