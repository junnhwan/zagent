# Sprint Contract

## 文档状态
- 状态：草稿
- 对应 Sprint：Sprint 3 - 后端模块化单体重构
- 最后更新时间：2026-03-26

> 说明：Sprint 2 合同已归档至 `artifacts/sprint_contract_sprint2.md`。

## 一、Sprint 目标
- 将后端从“按技术层堆文件”的结构，重整为“领域模块 + 模块内分层”的模块化单体结构，并确保现有对外 API 行为稳定（路径、请求体、响应体保持兼容）。

## 二、Included Scope
- 领域模块边界落地（以包结构为主，不拆 Maven module）：对齐 `product_spec.md` 中规划的 `agent/tooling/knowledge/runtime/observability/configcenter/web`。
- `web` 入口层收敛：
  - 将 `controller` 下的入口按领域归位到统一 `web` 包（建议结构：`web.agent` / `web.chat` / `web.rag` / `web.admin` / `web.mcp`）。
  - 对外契约（DTO/VO）随入口层归位（建议放置到 `web.contract` 或 `web.*.contract`），减少通用 `model/dto`、`model/vo` 的“全局共享”。
- 依赖方向约束（通过包结构与引用修复体现）：
  - `web` 只能依赖领域服务与契约，不反向依赖 `web`。
  - 领域服务不直接依赖具体 Controller/VO，必要时使用模块内模型或最小契约接口。
- 最小迁移原则：
  - 以“移动包 + 调整 Spring/MyBatis 扫描 + 修复引用”为主。
  - 不引入新功能、不改表结构、不做大范围重写。
- 回归验证：
  - `mvn "-DskipTests" compile` 必须通过。
  - 受影响的单元测试必须通过，固定测试清单为：`mvn "-Dtest=AiClientAssemblyServiceImplTest,McpTransportConfigParserImplTest,McpBindingResolverImplTest,ReActExecuteStrategyTest,FlowExecuteStrategyTest" test`。

## 三、最小迁移清单
- Controller 迁移（保持 API 路径不变）：
  - `AgentController` -> `io.wanjune.zagent.web.agent`
  - `ChatController` -> `io.wanjune.zagent.web.chat`
  - `RagController` -> `io.wanjune.zagent.web.rag`
  - `AdminController` -> `io.wanjune.zagent.web.admin`
- 高频契约迁移（共享模型收缩）：
  - `AgentRunRequest` -> `io.wanjune.zagent.agent.model.AgentRunRequest`
  - `AgentResultVO` -> `io.wanjune.zagent.agent.model.AgentResultVO`
- 引用修复范围：
  - `AgentService`
  - `AgentServiceImpl`
  - `AgentToolCallback`
  - 上述 Controller 的 import 与包声明

## 四、Excluded Scope
- 不拆微服务。
- 不强制升级为 Maven 多模块。
- 不修改数据库结构与存量数据。
- 不变更对外 API 路径与 wire contract（除非属于修复 bug 且能提供兼容策略与证据）。
- 不做 Observability 的真实数据链路接入（留到后续 Sprint）。

## 五、用户可感知行为
- 用户应能够：继续正常使用当前前端（至少能完成 Overview -> Playground -> 同步运行 -> 展示 steps 的主演示路径）。
- 用户应能够：不需要理解内部包迁移即可运行与演示系统（行为保持一致）。

## 六、验证计划
- 步骤 1：后端编译验证：执行 `mvn "-DskipTests" compile` 通过。
- 步骤 2：后端测试验证：执行固定测试集 `mvn "-Dtest=AiClientAssemblyServiceImplTest,McpTransportConfigParserImplTest,McpBindingResolverImplTest,ReActExecuteStrategyTest,FlowExecuteStrategyTest" test` 通过。
- 步骤 3：接口契约抽查：确认关键入口路径未变更（至少包含 `/api/agent/*`、`/api/chat/*`、`/api/rag/*`、`/api/admin/*`）。
- 步骤 4：字段级 wire contract 抽查：确认 `/api/agent/run` 的返回结构仍包含 `agentId`、`agentName`、`finalOutput`、`steps`，且 `steps[*]` 包含 `sequence`、`clientId`、`input`、`output`。
- 步骤 5：前端构建验证：执行 `frontend` 目录 `npm run build` 通过（确保后端接口变更未导致前端编译期依赖出错）。

## 七、风险与假设
- 风险：包迁移容易引入 Spring 扫描、MyBatis Mapper 扫描、序列化类型引用等隐性问题。
- 假设：本轮以“结构重整”为主，业务逻辑尽量不改；必要修改以“保持兼容”为唯一目标。

## 八、Definition of Done
- [ ] 合同中的核心用户行为已实现（对外接口行为不回退）
- [ ] `mvn "-DskipTests" compile` 通过
- [ ] 受影响测试集通过（命令与结果记录在 `artifacts/build_report.md`）
- [ ] 关键 API 路径与 wire contract 兼容
- [ ] 本轮排除项未被误做进来
- [ ] 已知问题记录在 `artifacts/build_report.md`
