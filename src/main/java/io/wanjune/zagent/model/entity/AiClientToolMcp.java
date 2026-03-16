package io.wanjune.zagent.model.entity;

import lombok.Data;
import java.util.Date;

/**
 * MCP(Model Context Protocol)工具配置
 * 对应表: ai_client_tool_mcp
 * 支持SSE和Stdio两种传输方式
 */
@Data
public class AiClientToolMcp {
    /** 主键ID */
    private Long id;
    /** MCP工具ID */
    private String mcpId;
    /** MCP名称 */
    private String mcpName;
    /** 传输类型 sse/stdio */
    private String transportType;
    /** 传输配置(JSON) */
    private String transportConfig;
    /** 请求超时(分钟) */
    private Integer requestTimeout;
    /** 状态 0-禁用 1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
