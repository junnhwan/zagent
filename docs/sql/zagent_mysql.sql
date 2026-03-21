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
  `step_prompt` text DEFAULT NULL COMMENT '步骤提示词模板(支持%s格式化参数,response_assistant支持JSON多变体)',
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
VALUES ('1001', 'https://apitl.de5.net', 'sk-F46mrbSXWRzZD2FLxsVMGrSpaxDzNF3HdGjkQuJlrYbOk0Ad', 'v1/chat/completions', 'v1/embeddings');

-- Model config
INSERT INTO `ai_client_model` (`model_id`, `api_id`, `model_name`, `model_type`)
VALUES ('2001', '1001', 'gpt-5.4', 'openai');

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
INSERT INTO `ai_agent_flow_config` (`agent_id`, `client_id`, `client_type`, `sequence`, `step_prompt`) VALUES
('2', '3002', 'task_analyzer', 1,
 '**原始用户需求:** %s\n**当前执行步骤:** 第 %d 步 (最大 %d 步)\n**历史执行记录:**\n%s\n**当前任务:** %s\n**分析要求:**\n请深入分析用户的具体需求，制定明确的执行策略：\n1. 理解用户真正想要什么（如：具体的学习计划、项目列表、技术方案等）\n2. 分析需要哪些具体的执行步骤（如：搜索信息、检索项目、生成内容等）\n3. 制定能够产生实际结果的执行策略\n4. 确保策略能够直接回答用户的问题\n**输出格式要求:**\n任务状态分析: [当前任务完成情况的详细分析]\n执行历史评估: [对已完成工作的质量和效果评估]\n下一步策略: [具体的执行计划，包括需要调用的工具和生成的内容]\n完成度评估: [0-100]%%\n任务状态: [CONTINUE/COMPLETED]'),
('2', '3003', 'precision_executor', 2,
 '**用户原始需求:** %s\n**分析师策略:** %s\n**执行指令:** 你是一个精准任务执行器，需要根据用户需求和分析师策略，实际执行具体的任务。\n**执行要求:**\n1. 直接执行用户的具体需求（如搜索、检索、生成内容等）\n2. 如果需要搜索信息，请实际进行搜索和检索\n3. 如果需要生成计划、列表等，请直接生成完整内容\n4. 提供具体的执行结果，而不只是描述过程\n5. 确保执行结果能直接回答用户的问题\n**输出格式:**\n执行目标: [明确的执行目标]\n执行过程: [实际执行的步骤和调用的工具]\n执行结果: [具体的执行成果和获得的信息/内容]\n质量检查: [对执行结果的质量评估]'),
('2', '3004', 'quality_supervisor', 3,
 '**用户原始需求:** %s\n**执行结果:** %s\n**监督要求:**\n请严格评估执行结果是否真正满足了用户的原始需求：\n1. 检查是否直接回答了用户的问题\n2. 评估内容的完整性和实用性\n3. 确认是否提供了用户期望的具体结果（如学习计划、项目列表等）\n4. 判断是否只是描述过程而没有给出实际答案\n**输出格式:**\n需求匹配度: [执行结果与用户原始需求的匹配程度分析]\n内容完整性: [内容是否完整、具体、实用]\n问题识别: [发现的问题和不足，特别是是否偏离了用户真正的需求]\n改进建议: [具体的改进建议，确保能直接满足用户需求]\n质量评分: [1-10分的质量评分]\n是否通过: [PASS/FAIL/OPTIMIZE]'),
('2', '3005', 'response_assistant', 4,
 '{"completed":"基于以下执行过程，请直接回答用户的原始问题，提供最终的答案和结果：\\n**用户原始问题:** %s\\n**执行历史和过程:**\\n%s\\n**要求:**\\n1. 直接回答用户的原始问题\\n2. 基于执行过程中获得的信息和结果\\n3. 提供具体、实用的最终答案\\n4. 如果是要求制定计划、列表等，请直接给出完整的内容\\n5. 避免只描述执行过程，重点是最终答案\\n6. 以MD语法的表格形式，优化展示结果数据\\n请直接给出用户问题的最终答案：","incomplete":"虽然任务未完全执行完成，但请基于已有的执行过程，尽力回答用户的原始问题：\\n**用户原始问题:** %s\\n**已执行的过程和获得的信息:**\\n%s\\n**要求:**\\n1. 基于已有信息，尽力回答用户的原始问题\\n2. 如果信息不足，说明哪些部分无法完成并给出原因\\n3. 提供已能确定的部分答案\\n4. 给出完成剩余部分的具体建议\\n5. 以MD语法的表格形式，优化展示结果数据\\n请基于现有信息给出用户问题的答案："}');

-- Flow策略Agent（工具编排: 分析→规划→执行）
INSERT INTO `ai_agent` (`agent_id`, `agent_name`, `description`, `channel`, `strategy`)
VALUES ('3', '工具编排Agent', 'MCP工具驱动的任务规划和执行', 'agent', 'flow');
INSERT INTO `ai_agent_flow_config` (`agent_id`, `client_id`, `client_type`, `sequence`, `step_prompt`) VALUES
('3', '3006', 'tool_mcp', 1,
 '# MCP工具能力分析任务\n\n## 重要说明\n**注意：本阶段仅进行MCP工具能力分析，不执行用户的实际请求。**\n这是一个纯分析阶段，目的是评估可用工具的能力和适用性，为后续的执行规划提供依据。\n\n## 用户请求\n%s\n\n## 分析要求\n请基于当前可用的MCP工具信息，针对用户请求进行详细的工具能力分析（仅分析，不执行）：\n\n### 1. 工具匹配分析\n- 分析每个可用工具的核心功能和适用场景\n- 评估哪些工具能够满足用户请求的具体需求\n- 标注每个工具的匹配度（高/中/低）\n\n### 2. 工具使用指南\n- 提供每个相关工具的具体调用方式\n- 说明必需的参数和可选参数\n- 给出参数的示例值和格式要求\n\n### 3. 执行策略建议\n- 推荐最优的工具组合方案\n- 建议工具的调用顺序和依赖关系\n- 提供备选方案和降级策略\n\n### 4. 注意事项\n- 标注工具的使用限制和约束条件\n- 提醒可能的错误情况和处理方式\n- 给出性能优化建议\n\n### 5. 分析总结\n- 明确说明这是分析阶段，不要执行任何实际操作\n- 总结工具能力评估结果\n- 为后续执行阶段提供建议\n\n请确保分析结果准确、详细、可操作，并再次强调这仅是分析阶段。'),
('3', '3007', 'planning', 2,
 '# 智能执行计划生成\n\n## 用户需求分析\n**完整用户请求：**\n%s\n\n**重要提醒：** 在生成执行计划时，必须完整保留和传递用户请求中的所有详细信息，包括但不限于：\n- 任务的具体目标和期望结果\n- 涉及的数据、参数、配置等详细信息\n- 特定的业务规则、约束条件或要求\n- 输出格式、质量标准或验收条件\n\n## MCP工具能力分析结果\n%s\n\n## 执行计划要求\n请基于上述用户详细需求和MCP工具分析结果，生成精确的执行计划：\n\n### 核心要求\n1. **完整保留用户需求**: 必须将用户请求中的所有详细信息完整传递到每个执行步骤中\n2. **严格遵循MCP分析结果**: 必须根据工具能力分析中的匹配度和推荐方案制定步骤\n3. **精确工具映射**: 每个步骤必须使用确切的函数名称\n4. **参数完整性**: 所有工具调用必须包含用户原始需求中的完整参数信息\n5. **依赖关系明确**: 合理安排步骤顺序，确保前置条件得到满足\n6. **合理粒度**: 避免过度细分，每个步骤应该是完整且独立的功能单元\n\n### 格式规范\n请使用以下Markdown格式生成3-5个执行步骤：\n\n### 第1步：[步骤描述]\n- **优先级**: [HIGH/MEDIUM/LOW]\n- **使用工具**: [必须使用确切的函数名称]\n- **依赖步骤**: [前置步骤序号，如无依赖则填写"无"]\n- **执行方法**: [具体执行策略，包含工具调用参数]\n- **预期输出**: [期望的结果]\n- **成功标准**: [判断任务完成的标准]\n\n### 第2步：[步骤描述]\n...\n\n### 质量检查清单\n生成计划后请确认：\n- 每个步骤都有明确的序号和描述\n- 使用了正确的Markdown格式\n- 工具选择恰当\n- 依赖关系清晰\n- 执行方法具体可操作\n- 成功标准明确可衡量\n\n现在请开始生成执行步骤规划：'),
('3', '3008', 'executor', 3,
 '你是一个智能执行助手，需要执行以下步骤:\n\n**步骤内容:**\n%s\n\n**用户原始请求:**\n%s\n\n%s\n\n**执行要求:**\n1. 仔细分析步骤内容，理解需要执行的具体任务\n2. 如果涉及MCP工具调用，请使用相应的工具\n3. 提供详细的执行过程和结果\n4. 如果遇到问题，请说明具体的错误信息\n5. 执行完成后，必须在回复末尾明确输出执行结果，格式如下:\n   === 执行结果 ===\n   状态: [成功/失败]\n   结果描述: [具体的执行结果描述]\n\n请开始执行这个步骤，并严格按照要求提供详细的执行报告和结果输出。');
