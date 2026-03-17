-- ============================================================
-- ZAgent MySQL Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS `zagent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `zagent`;

-- -----------------------------------------------------------
-- 1. ai_agent - Agent top-level config
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
  `agent_name` varchar(50) NOT NULL COMMENT '智能体名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `channel` varchar(32) DEFAULT NULL COMMENT '渠道类型(agent/chat_stream)',
  `strategy` varchar(64) DEFAULT 'fixed' COMMENT '执行策略(fixed/auto/flow)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI智能体配置表';

-- -----------------------------------------------------------
-- 2. ai_agent_flow_config - Agent execution pipeline
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_flow_config`;
CREATE TABLE `ai_agent_flow_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
  `client_id` varchar(64) NOT NULL COMMENT '客户端ID',
  `client_type` varchar(64) DEFAULT 'default' COMMENT '客户端角色(default/task_analyzer/precision_executor/quality_supervisor/response_assistant/tool_mcp/planning/executor)',
  `sequence` int NOT NULL COMMENT '序列号(执行顺序)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体执行流程配置表';

-- -----------------------------------------------------------
-- 3. ai_agent_task_schedule - Cron task scheduling
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_task_schedule`;
CREATE TABLE `ai_agent_task_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
  `task_name` varchar(64) DEFAULT NULL COMMENT '任务名称',
  `description` varchar(255) DEFAULT NULL COMMENT '任务描述',
  `cron_expression` varchar(50) NOT NULL COMMENT '时间表达式',
  `task_param` text COMMENT '任务入参配置(JSON)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:无效,1:有效)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体任务调度配置表';

-- -----------------------------------------------------------
-- 4. ai_client - AI client definitions
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_client`;
CREATE TABLE `ai_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_id` varchar(64) NOT NULL COMMENT '客户端ID',
  `client_name` varchar(50) NOT NULL COMMENT '客户端名称',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI客户端配置表';

-- -----------------------------------------------------------
-- 5. ai_client_api - OpenAI API endpoint configs
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_api`;
CREATE TABLE `ai_client_api` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `api_id` varchar(64) NOT NULL COMMENT 'API配置ID',
  `base_url` varchar(255) NOT NULL COMMENT 'API基础URL',
  `api_key` varchar(255) NOT NULL COMMENT 'API密钥',
  `completions_path` varchar(255) NOT NULL COMMENT '补全API路径',
  `embeddings_path` varchar(255) NOT NULL COMMENT '嵌入API路径',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_id` (`api_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OpenAI API配置表';

-- -----------------------------------------------------------
-- 6. ai_client_model - Chat model configs
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_model`;
CREATE TABLE `ai_client_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `model_id` varchar(64) NOT NULL COMMENT '模型ID',
  `api_id` varchar(64) NOT NULL COMMENT '关联API配置ID',
  `model_name` varchar(64) NOT NULL COMMENT '模型名称',
  `model_type` varchar(32) NOT NULL COMMENT '模型类型(openai/deepseek/claude)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天模型配置表';

-- -----------------------------------------------------------
-- 7. ai_client_system_prompt - System prompts
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_system_prompt`;
CREATE TABLE `ai_client_system_prompt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prompt_id` varchar(64) NOT NULL COMMENT '提示词ID',
  `prompt_name` varchar(50) NOT NULL COMMENT '提示词名称',
  `prompt_content` text NOT NULL COMMENT '提示词内容',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prompt_id` (`prompt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统提示词配置表';

-- -----------------------------------------------------------
-- 8. ai_client_advisor - Advisor configs
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_advisor`;
CREATE TABLE `ai_client_advisor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `advisor_id` varchar(64) NOT NULL COMMENT '顾问ID',
  `advisor_name` varchar(50) NOT NULL COMMENT '顾问名称',
  `advisor_type` varchar(50) NOT NULL COMMENT '顾问类型(ChatMemory/RagAnswer)',
  `order_num` int DEFAULT '0' COMMENT '顺序号',
  `ext_param` varchar(2048) DEFAULT NULL COMMENT '扩展参数(JSON)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_advisor_id` (`advisor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='顾问配置表';

-- -----------------------------------------------------------
-- 9. ai_client_tool_mcp - MCP tool connections
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_tool_mcp`;
CREATE TABLE `ai_client_tool_mcp` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mcp_id` varchar(64) NOT NULL COMMENT 'MCP工具ID',
  `mcp_name` varchar(50) NOT NULL COMMENT 'MCP名称',
  `transport_type` varchar(20) NOT NULL COMMENT '传输类型(sse/stdio)',
  `transport_config` varchar(1024) DEFAULT NULL COMMENT '传输配置(JSON)',
  `request_timeout` int DEFAULT '180' COMMENT '请求超时(分钟)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mcp_id` (`mcp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MCP客户端配置表';

-- -----------------------------------------------------------
-- 10. ai_client_config - Central relation table
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_config`;
CREATE TABLE `ai_client_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_type` varchar(32) NOT NULL COMMENT '源类型(model/client)',
  `source_id` varchar(64) NOT NULL COMMENT '源ID',
  `target_type` varchar(32) NOT NULL COMMENT '目标类型(api/model/prompt/advisor/tool_mcp)',
  `target_id` varchar(64) NOT NULL COMMENT '目标ID',
  `ext_param` varchar(1024) DEFAULT NULL COMMENT '扩展参数(JSON)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_type`, `source_id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI客户端统一关联配置表';

-- ============================================================
-- Sample Data
-- ============================================================

-- API config
INSERT INTO `ai_client_api` (`api_id`, `base_url`, `api_key`, `completions_path`, `embeddings_path`)
VALUES ('1001', 'https://ai.qaq.al', 'sk-xxxx', 'v1/chat/completions', 'v1/embeddings');

-- Model config
INSERT INTO `ai_client_model` (`model_id`, `api_id`, `model_name`, `model_type`)
VALUES ('2001', '1001', 'gpt-4.1-mini', 'openai');

-- Client configs (多个角色)
INSERT INTO `ai_client` (`client_id`, `client_name`, `description`) VALUES
('3001', '通用对话', '通用AI对话客户端'),
('3002', '任务分析器', 'Auto策略-任务分析客户端'),
('3003', '精确执行器', 'Auto策略-精确执行客户端'),
('3004', '质量监督员', 'Auto策略-质量监督客户端'),
('3005', '响应助手', 'Auto策略-最终总结客户端'),
('3006', 'MCP工具客户端', 'Flow策略-MCP工具调用客户端'),
('3007', '规划客户端', 'Flow策略-任务规划客户端'),
('3008', '执行客户端', 'Flow策略-步骤执行客户端');

-- Advisor config
INSERT INTO `ai_client_advisor` (`advisor_id`, `advisor_name`, `advisor_type`, `order_num`, `ext_param`)
VALUES ('4001', '对话记忆', 'ChatMemory', 1, '{"maxMessages": 200}');

-- System prompts (各角色)
INSERT INTO `ai_client_system_prompt` (`prompt_id`, `prompt_name`, `prompt_content`, `description`) VALUES
('6001', '通用助手', '你是一个专业的AI助手，请用中文回答用户的问题。', '通用对话提示词'),
('6002', '任务分析器', '你是一个任务分析专家。分析用户需求，将复杂任务分解为可执行的步骤，明确每一步的目标和所需资源。输出格式：\n【任务理解】...\n【分析结果】...\n【建议操作】...', 'Auto策略-分析提示词'),
('6003', '精确执行器', '你是一个精确的任务执行者。基于分析结果，严格按照要求执行任务，输出详细的执行结果。', 'Auto策略-执行提示词'),
('6004', '质量监督员', '你是一个严格的质量审核专家。评估执行结果的质量，判断是否满足原始需求。\n输出格式：\n【评估结果】PASS/FAIL/OPTIMIZE\n【详细说明】...\n【改进建议】...（FAIL/OPTIMIZE时提供）', 'Auto策略-监督提示词'),
('6005', '响应助手', '你是一个总结专家。根据执行历史，生成一份简洁清晰的最终报告，直接回答用户的原始问题。', 'Auto策略-总结提示词'),
('6006', '规划客户端', '你是一个任务规划专家。根据用户需求和可用工具，制定详细的执行计划。输出格式：\n### 第1步：...\n### 第2步：...\n每一步需明确目标和使用的工具。', 'Flow策略-规划提示词');

-- Config relations: client -> model/prompt/advisor
INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`) VALUES
('client', '3001', 'model', '2001'),
('client', '3001', 'prompt', '6001'),
('client', '3001', 'advisor', '4001'),
('client', '3002', 'model', '2001'),
('client', '3002', 'prompt', '6002'),
('client', '3003', 'model', '2001'),
('client', '3003', 'prompt', '6003'),
('client', '3004', 'model', '2001'),
('client', '3004', 'prompt', '6004'),
('client', '3005', 'model', '2001'),
('client', '3005', 'prompt', '6005'),
('client', '3006', 'model', '2001'),
('client', '3007', 'model', '2001'),
('client', '3007', 'prompt', '6006'),
('client', '3008', 'model', '2001');

-- ========== Agent configs (三种策略示例) ==========

-- Fixed策略Agent
INSERT INTO `ai_agent` (`agent_id`, `agent_name`, `description`, `channel`, `strategy`)
VALUES ('1', '通用对话Agent', '通用AI对话', 'chat_stream', 'fixed');
INSERT INTO `ai_agent_flow_config` (`agent_id`, `client_id`, `client_type`, `sequence`) VALUES
('1', '3001', 'default', 1);

-- Auto策略Agent（智能编排: 分析→执行→监督→总结）
INSERT INTO `ai_agent` (`agent_id`, `agent_name`, `description`, `channel`, `strategy`)
VALUES ('2', '智能分析Agent', '自动分析执行和质量监督', 'agent', 'auto');
INSERT INTO `ai_agent_flow_config` (`agent_id`, `client_id`, `client_type`, `sequence`) VALUES
('2', '3002', 'task_analyzer', 1),
('2', '3003', 'precision_executor', 2),
('2', '3004', 'quality_supervisor', 3),
('2', '3005', 'response_assistant', 4);

-- Flow策略Agent（工具编排: 分析→规划→执行）
INSERT INTO `ai_agent` (`agent_id`, `agent_name`, `description`, `channel`, `strategy`)
VALUES ('3', '工具编排Agent', 'MCP工具驱动的任务规划和执行', 'agent', 'flow');
INSERT INTO `ai_agent_flow_config` (`agent_id`, `client_id`, `client_type`, `sequence`) VALUES
('3', '3006', 'tool_mcp', 1),
('3', '3007', 'planning', 2),
('3', '3008', 'executor', 3);
