-- ============================================================
-- ZAgent - Git Repository MCP Setup
-- ============================================================
-- 用途说明：
-- 1) 本文件为 git-repo MCP 服务的 MySQL 初始化数据。
-- 2) 包含 MCP 工具定义、模型定义 和 model->tool_mcp 绑定关系。
-- 3) 注意：从 2026-03-26 起，MCP 配置统一通过 `application.yml`
--    中的 `zagent.mcp.sync.manifest` 自动同步，
--    本文件仅作为手动初始化 / 参考使用。
-- ============================================================

USE `zagent`;

-- -----------------------------------------------------------
-- 1. ai_client_tool_mcp - Git Repository MCP 工具
-- -----------------------------------------------------------
INSERT INTO `ai_client_tool_mcp` (`mcp_id`, `mcp_name`, `transport_type`, `transport_config`, `request_timeout`, `status`)
VALUES (
  '5004',
  'git-repo',
  'sse',
  '{"baseUri":"http://127.0.0.1:18082","sseEndpoint":"/sse"}',
  10,
  1
);

-- -----------------------------------------------------------
-- 2. ai_client_model - 绑定 git-repo 的模型
-- -----------------------------------------------------------
INSERT INTO `ai_client_model` (`model_id`, `api_id`, `model_name`, `model_type`, `status`)
VALUES ('2006', '1001', 'gpt-5.4', 'openai', 1);

-- -----------------------------------------------------------
-- 3. ai_client_config - model 2006 -> tool_mcp 5004 绑定
-- -----------------------------------------------------------
INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`, `status`)
VALUES ('model', '2006', 'tool_mcp', '5004', 1);
