-- ============================================================
-- ZAgent Dual MCP Setup
-- 目标：让 Flow Agent(agent_id=3) 同时挂载 stdio + SSE 两种 MCP
-- 说明：执行一次后，后续无需在 2002/2003 间来回切换
-- 前提：
--   1) Windows 已安装 Node.js，且 D:\node.js\npx.cmd 可用
--   2) 需要测试 SSE 时，先启动：python tools/mcp_sse_probe.py
-- ============================================================

USE `zagent`;

-- 1) 新增一个“双 MCP 专用模型”，避免影响现有单项调试模型
INSERT INTO `ai_client_model` (`model_id`, `api_id`, `model_name`, `model_type`, `status`)
VALUES ('2004', '1001', 'gpt-5.4', 'openai', 1)
ON DUPLICATE KEY UPDATE
  `api_id` = VALUES(`api_id`),
  `model_name` = VALUES(`model_name`),
  `model_type` = VALUES(`model_type`),
  `status` = VALUES(`status`);

-- 2) 确保 stdio MCP 工具存在
INSERT INTO `ai_client_tool_mcp` (`mcp_id`, `mcp_name`, `transport_type`, `transport_config`, `request_timeout`, `status`)
VALUES (
  '5001',
  'filesystem-docs',
  'stdio',
  '{"filesystem":{"command":"D:\\\\node.js\\\\npx.cmd","args":["-y","@modelcontextprotocol/server-filesystem","D:\\\\dev\\\\my_proj\\\\zagent\\\\docs"],"env":{"MCP_LOG_LEVEL":"info"}}}',
  10,
  1
)
ON DUPLICATE KEY UPDATE
  `mcp_name` = VALUES(`mcp_name`),
  `transport_type` = VALUES(`transport_type`),
  `transport_config` = VALUES(`transport_config`),
  `request_timeout` = VALUES(`request_timeout`),
  `status` = VALUES(`status`);

-- 3) 确保 SSE MCP 工具存在
INSERT INTO `ai_client_tool_mcp` (`mcp_id`, `mcp_name`, `transport_type`, `transport_config`, `request_timeout`, `status`)
VALUES (
  '5002',
  'sse-probe',
  'sse',
  '{"baseUri":"http://127.0.0.1:18080","sseEndpoint":"/sse"}',
  10,
  1
)
ON DUPLICATE KEY UPDATE
  `mcp_name` = VALUES(`mcp_name`),
  `transport_type` = VALUES(`transport_type`),
  `transport_config` = VALUES(`transport_config`),
  `request_timeout` = VALUES(`request_timeout`),
  `status` = VALUES(`status`);

-- 4) 将两个 MCP 都绑定到双 MCP 模型 2004
INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
SELECT 'model', '2004', 'tool_mcp', '5001'
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_client_config`
  WHERE `source_type` = 'model' AND `source_id` = '2004' AND `target_type` = 'tool_mcp' AND `target_id` = '5001'
);

INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
SELECT 'model', '2004', 'tool_mcp', '5002'
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_client_config`
  WHERE `source_type` = 'model' AND `source_id` = '2004' AND `target_type` = 'tool_mcp' AND `target_id` = '5002'
);

-- 5) 让 Flow Agent 的 tool_mcp / executor 两个 client 统一走 2004
DELETE FROM `ai_client_config`
WHERE `source_type` = 'client'
  AND `source_id` IN ('3006', '3008')
  AND `target_type` = 'model';

INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
VALUES
  ('client', '3006', 'model', '2004'),
  ('client', '3008', 'model', '2004');

-- 6) 检查结果
SELECT * FROM `ai_client_tool_mcp` WHERE `mcp_id` IN ('5001', '5002');
SELECT * FROM `ai_client_config`
WHERE (`source_type` = 'model' AND `source_id` = '2004')
   OR (`source_type` = 'client' AND `source_id` IN ('3006', '3008'))
ORDER BY `source_type`, `source_id`, `target_type`, `target_id`;

-- 7) 使用建议
-- SSE 定向测试：
--   必须优先调用 read_probe 工具读取 mcp_probe.txt，只返回文件名和测试码，不要猜测。
-- stdio 定向测试：
--   必须使用 filesystem MCP 工具读取 docs/mcp_probe.txt，只返回文件名和测试码，不要猜测。
