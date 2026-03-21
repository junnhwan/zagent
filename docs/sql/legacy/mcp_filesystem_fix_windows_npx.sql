USE `zagent`;

UPDATE `ai_client_tool_mcp`
SET `transport_config` = '{"filesystem":{"command":"D:\\\\node.js\\\\npx.cmd","args":["-y","@modelcontextprotocol/server-filesystem","D:\\\\dev\\\\my_proj\\\\zagent\\\\docs"],"env":{"MCP_LOG_LEVEL":"info"}}}'
WHERE `mcp_id` = '5001';

SELECT `mcp_id`, `mcp_name`, `transport_type`, `transport_config`
FROM `ai_client_tool_mcp`
WHERE `mcp_id` = '5001';
