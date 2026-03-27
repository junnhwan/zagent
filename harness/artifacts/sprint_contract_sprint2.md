# Sprint Contract（归档）

## 文档状态
- 状态：已归档
- 对应 Sprint：Sprint 2 - Agent 核心编排体验强化
- 最后更新时间：2026-03-26

> 说明：为了继续起草后续 Sprint，本文件保留 Sprint 2 合同原文作为归档证据。

## 一、Sprint 目标
- 让用户在 `Playground/Workflows` 中能够用“可见化”的方式讲清三类 Agent 编排范式：`Plan and Execute`、`ReAct`、`Reflection`，并且能在一次演示运行中看到“策略/范式 + 步骤化过程 + 最终输出”。

## 二、Included Scope
- `Playground`：
  - 在现有 Agent 运行入口基础上，新增“编排范式视角”选择（Plan&Execute / ReAct / Reflection），并在界面中清晰显示用户当前选择。
  - 将运行结果展示拆分为三块：选择的范式/策略、最终输出、步骤列表（step list）。
  - 步骤列表必须基于后端返回的 `AgentResultVO.steps`（至少展示 `sequence/clientId/input/output` 四项），并支持展开/折叠。
  - 提供“复制结果/复制步骤 JSON”的演示友好能力（便于面试讲解与复盘）。
- `Workflows`：
  - 将三类范式的“讲解卡片”升级为“可操作入口”，每张卡片包含：适用场景、风险点、推荐演示按钮（跳转到 Playground 并预选对应视角）。
  - 明确说明“视角”与“现有策略实现”的映射关系（允许先做文案映射，不要求后端策略全面对齐）。
- 兼容与一致性：
  - 现有 `/agent-test` 旧入口可以保留，但演示主路径必须以 `Playground` 为准。
  - Sprint 2 触达范围内中文文案必须无乱码。

## 三、Excluded Scope
- 不做后端模块化重构（包结构与模块拆分留到 Sprint 3）。
- 不做数据库结构变更。
- 不做 Settings 全量重做（仅允许为了展示一致性做少量文案/入口调整）。
- 不强制完成真实 Observability 数据链路（可以只在前端展示 `steps` 与必要的运行摘要）。

## 四、用户可感知行为
- 用户应能够：在 Playground 选择一个“编排范式视角”，并发起一次 Agent 运行。
- 用户应能够：看到该次运行的步骤化过程（step list）以及最终输出，并能用这些内容解释“Agent 是怎么得到结果的”。
- 用户应能够：在 Workflows 页面用 1 分钟解释三种范式差异，并一键跳转到对应演示入口。

## 五、验证计划
- 步骤 1：执行 `frontend` 目录下 `npm run build`，构建必须通过。
- 步骤 2：验证 `Playground` 存在“范式视角选择”，且选择状态在界面上可见。
- 步骤 3：触发一次 Agent 运行后，验证结果区域包含：
  - 最终输出（final output）；
  - 步骤列表（至少 1 条，展示 `sequence/clientId/input/output`）；
  - 可复制能力（复制最终输出或复制步骤 JSON，二选一也可，但必须有一个）。
- 步骤 4：验证 Workflows 的三张范式卡片均包含“跳转演示”入口，且跳转后能预选对应视角（以 URL query 或本地状态实现均可）。
- 步骤 5：检查 Sprint 2 触达范围内中文文案无乱码。

## 六、风险与假设
- 风险：如果后端某些策略不产出 `steps`，会导致可见化不足。
- 假设：至少一种可用 Agent/策略在现有实现下能够返回 `AgentResultVO.steps`，满足演示闭环；若不足，允许 Sprint 2 内做“最小后端增强”以补齐 `steps`（不改 DB，不做大重构）。

## 七、Definition of Done
- [ ] 合同中的核心用户行为已实现
- [ ] `frontend` 构建通过
- [ ] Playground 的“范式视角选择 + 步骤列表 + 最终输出”可演示
- [ ] Workflows 的“范式卡片 -> 演示跳转”可用
- [ ] 本轮排除项未被误做进来
- [ ] 已知问题记录在 `harness/artifacts/build_report.md`

