-- ============================================================
-- ZAgent MCP Filesystem Quick Setup
-- 目标：让 Flow Agent(agent_id=3) 能通过 MCP 读取 docs/ 目录
-- 前提：本机已安装 Node.js 且可执行 npx
--      npx -y @modelcontextprotocol/server-filesystem D:\dev\my_proj\zagent\docs
-- ============================================================

USE `zagent`;

-- 1) 新增一个专用的 MCP 模型，避免影响已有普通聊天模型 2001
INSERT INTO `ai_client_model` (`model_id`, `api_id`, `model_name`, `model_type`, `status`)
VALUES ('2002', '1001', 'gpt-5.4', 'openai', 1)
ON DUPLICATE KEY UPDATE
  `api_id` = VALUES(`api_id`),
  `model_name` = VALUES(`model_name`),
  `model_type` = VALUES(`model_type`),
  `status` = VALUES(`status`);

-- 2) 新增 filesystem MCP 工具
INSERT INTO `ai_client_tool_mcp` (`mcp_id`, `mcp_name`, `transport_type`, `transport_config`, `request_timeout`, `status`)
VALUES (
  '5001',
  'filesystem-docs',
  'stdio',
  '{"filesystem":{"command":"npx","args":["-y","@modelcontextprotocol/server-filesystem","D:\\\\dev\\\\my_proj\\\\zagent\\\\docs"],"env":{"MCP_LOG_LEVEL":"info"}}}',
  10,
  1
)
ON DUPLICATE KEY UPDATE
  `mcp_name` = VALUES(`mcp_name`),
  `transport_type` = VALUES(`transport_type`),
  `transport_config` = VALUES(`transport_config`),
  `request_timeout` = VALUES(`request_timeout`),
  `status` = VALUES(`status`);

-- 3) 把 tool_mcp 绑定到专用模型 2002
INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
SELECT 'model', '2002', 'tool_mcp', '5001'
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_client_config`
  WHERE `source_type` = 'model' AND `source_id` = '2002' AND `target_type` = 'tool_mcp' AND `target_id` = '5001'
);

-- 4) 让 Flow Agent 的 tool_mcp / executor 两个 client 使用带 MCP 的模型 2002
DELETE FROM `ai_client_config`
WHERE `source_type` = 'client'
  AND `source_id` IN ('3006', '3008')
  AND `target_type` = 'model';

INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
VALUES
  ('client', '3006', 'model', '2002'),
  ('client', '3008', 'model', '2002');

-- 5) 检查结果
SELECT * FROM `ai_client_tool_mcp` WHERE `mcp_id` = '5001';
SELECT * FROM `ai_client_config` WHERE `source_id` IN ('2002', '3006', '3008') ORDER BY `source_type`, `source_id`, `target_type`;

-- 6) 验证建议
-- 重启后端后，在 docs/ 下新建 mcp_probe.txt，内容写：本次MCP测试码: 7QX-314159
-- 前端 Agent 页使用 agentId=3 提问：
-- 必须使用 MCP 工具读取 docs/mcp_probe.txt，返回文件名和测试码，不要猜测。
