# Sprint Contract（归档）

## 文档状态
- 状态：已归档
- 对应 Sprint：Sprint 3 - 后端模块化单体重构
- 最后更新时间：2026-03-26

> 说明：为了继续起草后续 Sprint，本文件保留 Sprint 3 合同原文作为归档证据。

## 一、Sprint 目标
- 在不拆 Maven 多模块、不改数据库结构的前提下，将当前后端从“技术层目录混放”收敛为“模块化单体”结构，让目录、控制器入口、服务边界与领域叙事一致，便于演示、维护与后续扩展。

## 二、Included Scope
- 目录与包结构
  - 引入领域化包边界：`agent`、`tooling`、`knowledge`、`runtime`、`configcenter`、`web`。
  - 本轮优先做“包归位”和“命名收敛”，不强求一步到位完成所有模型内聚。
- Controller 归位
  - 将 `controller` 下的入口按领域归位到 `web` 下的子包，例如：`web.agent`、`web.chat`、`web.knowledge`、`web.admin`。
  - 对外 API 路径保持不变，避免前端联动破坏。
- 共享模型收缩
  - 从全局 `model/dto|vo|entity|enums` 中选取至少一组高频契约，迁移到更贴近领域的包中。
  - 优先迁移本轮触达的契约，例如 `AgentRunRequest`、`AgentResultVO`。
- 依赖方向整理
  - 减少控制器直接感知过多底层细节。
  - 保持 Mapper、Service、Strategy 的依赖关系清晰，避免跨领域随意引用。
- 最小回归保障
  - 受影响测试需保持通过；至少执行编译与受影响模块测试。

## 三、最小迁移清单
- Controller 迁移（保持 API 路径不变）：
  - `AgentController` -> `io.wanjune.zagent.web.agent`
  - `ChatController` -> `io.wanjune.zagent.web.chat`
  - `RagController` -> `io.wanjune.zagent.web.rag`
  - `AdminController` -> `io.wanjune.zagent.web.admin`
- 高频契约迁移（共享模型收缩）：
  - `AgentRunRequest` -> `io.wanjune.zagent.agent.model.AgentRunRequest`
  - `AgentResultVO` -> `io.wanjune.zagent.agent.model.AgentResultVO`

## 四、Excluded Scope
- 不拆微服务。
- 不升级为 Maven 多模块。
- 不修改数据库表结构或 MyBatis XML 语义。
- 不大规模重写业务逻辑。
- 不将所有 `model/*` 一次性迁移完毕，只做本轮触达范围内的“最小有效收敛”。

## 五、验证计划
- 步骤 1：执行 `mvn "-DskipTests" compile`，编译必须通过。
- 步骤 2：执行固定测试清单 `mvn "-Dtest=AiClientAssemblyServiceImplTest,McpTransportConfigParserImplTest,McpBindingResolverImplTest,ReActExecuteStrategyTest,FlowExecuteStrategyTest" test`，必须通过。
- 步骤 3：检查原有核心 API 路径保持不变，例如 `/api/agent/run`、`/api/chat`、`/api/rag/*`、`/api/admin/*`。
- 步骤 4：字段级抽查 `/api/agent/run` 返回仍包含 `agentId`、`agentName`、`finalOutput`、`steps[*].sequence/clientId/input/output`。
- 步骤 5：`frontend` 目录 `npm run build` 通过。

## 六、Definition of Done
- [ ] 合同中的核心重构目标已实现
- [ ] `mvn "-DskipTests" compile` 通过
- [ ] 固定测试清单通过
- [ ] 原有核心 API 路径保持兼容
- [ ] 字段级 wire contract 保持兼容
- [ ] 已知问题记录在 `artifacts/build_report.md`
