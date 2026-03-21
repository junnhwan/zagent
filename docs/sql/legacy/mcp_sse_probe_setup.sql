-- ============================================================
-- ZAgent MCP SSE Probe Quick Setup
-- 目标：让 Flow Agent(agent_id=3) 通过 SSE MCP 读取 docs/ 目录
-- 前提：先启动本地 SSE MCP Probe 服务
--      python tools/mcp_sse_probe.py
-- 默认地址： http://127.0.0.1:18080/sse
-- ============================================================

USE `zagent`;

-- 1) 新增一个专用的 MCP 模型，避免影响已有普通聊天模型/stdio MCP 模型
INSERT INTO `ai_client_model` (`model_id`, `api_id`, `model_name`, `model_type`, `status`)
VALUES ('2003', '1001', 'gpt-5.4', 'openai', 1)
ON DUPLICATE KEY UPDATE
  `api_id` = VALUES(`api_id`),
  `model_name` = VALUES(`model_name`),
  `model_type` = VALUES(`model_type`),
  `status` = VALUES(`status`);

-- 2) 新增 SSE MCP 工具
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

-- 3) 把 SSE MCP 绑定到专用模型 2003
INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
SELECT 'model', '2003', 'tool_mcp', '5002'
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_client_config`
  WHERE `source_type` = 'model' AND `source_id` = '2003' AND `target_type` = 'tool_mcp' AND `target_id` = '5002'
);

-- 4) 让 Flow Agent 的 tool_mcp / executor 两个 client 使用带 SSE MCP 的模型 2003
DELETE FROM `ai_client_config`
WHERE `source_type` = 'client'
  AND `source_id` IN ('3006', '3008')
  AND `target_type` = 'model';

INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
VALUES
  ('client', '3006', 'model', '2003'),
  ('client', '3008', 'model', '2003');

-- 5) 检查结果
SELECT * FROM `ai_client_tool_mcp` WHERE `mcp_id` = '5002';
SELECT * FROM `ai_client_config` WHERE `source_id` IN ('2003', '3006', '3008') ORDER BY `source_type`, `source_id`, `target_type`;

-- 6) 验证建议
-- 先确保 docs/mcp_probe.txt 存在，内容示例：本次SSE-MCP测试码: SSE-928374
-- 后端重启后，前端 Agent 页使用 agentId=3 提问：
-- 必须使用 MCP 工具读取 mcp_probe.txt，返回文件名和测试码，不要猜测。
