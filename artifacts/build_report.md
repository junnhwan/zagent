# Build Report

## 文档状态
- 对应 Sprint：Sprint 3 - 后端模块化单体重构
- 最后更新时间：2026-03-26
- 结论：待 Evaluator 最终 QA

## 一、实现结论
- 已完成后端“最小有效重构”：控制器从 `controller` 归位到 `web` 分层包，保持所有对外 API 路径不变。
- 已收缩一组高频共享契约：`AgentRunRequest`、`AgentResultVO` 从全局 `model` 迁移到 `agent.model`，并完成全量引用修复。
- 本轮未改数据库结构、未引入新业务能力、未变更接口 wire contract。

## 二、已实现内容
- 控制器归位（路径保持不变）：
  - `AgentController` -> `io.wanjune.zagent.web.agent`
  - `ChatController` -> `io.wanjune.zagent.web.chat`
  - `RagController` -> `io.wanjune.zagent.web.rag`
  - `AdminController` -> `io.wanjune.zagent.web.admin`
- 契约迁移（共享模型收缩）：
  - `AgentRunRequest` -> `io.wanjune.zagent.agent.model.AgentRunRequest`
  - `AgentResultVO` -> `io.wanjune.zagent.agent.model.AgentResultVO`
- 依赖修复：
  - 已修复 `AgentService`、`AgentServiceImpl`、`AgentToolCallback`、`AgentController` 等关键引用。

## 三、自测与验证
- 编译验证：
  - `mvn "-DskipTests" compile` 通过。
- 受影响测试验证（策略 + MCP + 装配）：
  - `mvn "-Dtest=AiClientAssemblyServiceImplTest,McpTransportConfigParserImplTest,McpBindingResolverImplTest,ReActExecuteStrategyTest,FlowExecuteStrategyTest" test` 通过。
  - 结果：`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- 前端兼容性验证：
  - `frontend` 目录执行 `npm run build` 通过（已清理 `frontend/dist`）。
- 接口兼容性验证：
  - `@RequestMapping` 关键前缀保持不变：`/api/agent`、`/api/chat`、`/api/rag`、`/api/admin`。

## 四、已知问题
- Maven 输出仍包含历史依赖的弃用告警（与本轮重构无直接关系）。
- Mockito 动态 agent 告警仍存在（测试通过，不阻塞本 Sprint 验收）。
- 前端构建仍有 chunk size 警告（历史问题，非本轮重构引入）。
