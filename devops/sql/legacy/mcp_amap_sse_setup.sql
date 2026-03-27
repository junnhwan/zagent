-- ============================================================
-- ZAgent Amap SSE MCP Setup
-- 目标：为 Flow Agent(agent_id=3) 接入高德天气 + POI 搜索 MCP
-- 前提：
--   1) 已设置环境变量 AMAP_WEB_API_KEY
--   2) 已启动：python tools/amap_sse_mcp.py
--   3) 默认地址：http://127.0.0.1:18081/sse
-- ============================================================

USE `zagent`;

-- 1) 新增一个 Amap SSE 专用模型
INSERT INTO `ai_client_model` (`model_id`, `api_id`, `model_name`, `model_type`, `status`)
VALUES ('2005', '1001', 'gpt-5.4', 'openai', 1)
ON DUPLICATE KEY UPDATE
  `api_id` = VALUES(`api_id`),
  `model_name` = VALUES(`model_name`),
  `model_type` = VALUES(`model_type`),
  `status` = VALUES(`status`);

-- 2) 新增高德 SSE MCP 工具
INSERT INTO `ai_client_tool_mcp` (`mcp_id`, `mcp_name`, `transport_type`, `transport_config`, `request_timeout`, `status`)
VALUES (
  '5003',
  'amap-sse',
  'sse',
  '{"baseUri":"http://127.0.0.1:18081","sseEndpoint":"/sse"}',
  20,
  1
)
ON DUPLICATE KEY UPDATE
  `mcp_name` = VALUES(`mcp_name`),
  `transport_type` = VALUES(`transport_type`),
  `transport_config` = VALUES(`transport_config`),
  `request_timeout` = VALUES(`request_timeout`),
  `status` = VALUES(`status`);

-- 3) 将 Amap MCP 绑定到模型 2005
INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
SELECT 'model', '2005', 'tool_mcp', '5003'
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_client_config`
  WHERE `source_type` = 'model' AND `source_id` = '2005' AND `target_type` = 'tool_mcp' AND `target_id` = '5003'
);

-- 4) 让 Flow Agent 的 tool_mcp / executor 两个 client 使用带 Amap MCP 的模型 2005
DELETE FROM `ai_client_config`
WHERE `source_type` = 'client'
  AND `source_id` IN ('3006', '3008')
  AND `target_type` = 'model';

INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`)
VALUES
  ('client', '3006', 'model', '2005'),
  ('client', '3008', 'model', '2005');

-- 5) 检查结果
SELECT * FROM `ai_client_tool_mcp` WHERE `mcp_id` = '5003';
SELECT * FROM `ai_client_config` WHERE `source_id` IN ('2005', '3006', '3008') ORDER BY `source_type`, `source_id`, `target_type`;

-- 6) 手工测试建议
-- 天气：必须使用 amap_weather 工具查询上海今天天气，只返回核心结果，不要猜测。
-- POI：必须使用 amap_search_poi 工具查找杭州西湖附近咖啡店，只返回前3个结果。
