USE `zagent`;

UPDATE `ai_client_tool_mcp`
SET `transport_config` = '{"baseUri":"http://127.0.0.1:18080","sseEndpoint":"/sse"}'
WHERE `mcp_id` = '5002';

SELECT `mcp_id`, `mcp_name`, `transport_type`, `transport_config`
FROM `ai_client_tool_mcp`
WHERE `mcp_id` = '5002';
