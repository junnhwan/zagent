# Sprint Contract

## 文档状态
- 状态：草稿
- 对应 Sprint：Sprint 4 - Observability 与简历化收尾
- 最后更新时间：2026-03-26

## 一、Sprint 目标
- 让项目在“演示与简历呈现”上闭环：用户能在 Observability 中拿到一次运行的证据（steps + 运行态信息），并且 README 与演示脚本能支撑 3~5 分钟面试讲解。

## 二、Included Scope
- Observability 页面增强（以演示可用为第一优先级）
  - 能展示“最近一次 Playground 同步运行”的结果摘要（至少包含：范式视角、Agent、最终输出摘要、steps 数量）。
  - 能展示 steps 明细（复用 `AgentResultVO.steps`，至少展示 `sequence/clientId/input/output`）。
  - 提供至少一种“演示友好导出能力”：复制 steps JSON 或下载为文件（二选一即可，但必须可用）。
  - 最近一次运行数据来源固定为前端 `localStorage`，固定 key 为 `za.lastRun`，由 Playground 同步运行成功后写入。
  - `za.lastRun` 最小数据结构固定为：`lens`、`agentId`、`agentName`、`finalOutput`、`steps`、`createdAt`。
  - 当 `za.lastRun` 不存在或解析失败时，Observability 必须显示“未发现运行记录 + 引导去 Playground 运行一次”，不得出现空白页。
- 运行态可见性（只做现有能力聚合，不新增复杂链路）
  - MCP runtime status 使用现有 `mcpApi.runtimeStatus` 接口，若请求失败显示“不可用/请求失败”，不阻塞核心验收。
  - RAG tags 使用现有 `ragApi.tags` 接口，若请求失败显示“不可用/请求失败”，不阻塞核心验收。
- README 重写（简历型叙事）
  - README 必须包含：一句话定位、能力地图（Plan&Execute/ReAct/Reflection/Tool Use/RAG/Observability）、North Star Demo 路径、运行方式（后端/前端命令）、后端模块边界（`agent/tooling/knowledge/runtime/observability/configcenter/web`）与讲解要点。
- 演示脚本沉淀
  - 提供一份 3~5 分钟演示脚本，包含“先讲什么、后演示什么、演示时说什么、常见追问怎么答”。
  - 演示脚本文档路径固定为 `docs/demo_script.md`。

## 三、Excluded Scope
- 不新增与主线无关功能。
- 不做新的后端模块化/大重构，不改数据库结构。
- 不引入新的前端框架或大规模视觉重做。
- 不强制接入完整 trace/tool-call 后端链路（本 Sprint 以 steps + 运行态聚合为主）。

## 四、用户可感知行为
- 用户应能够：在 Playground 跑完一次同步运行后，到 Observability 看到该次运行的 steps 证据，并可复制/导出。
- 用户应能够：在 README 的指引下完成一次演示（从启动到跑通 Demo）并知道项目亮点在哪里。

## 五、验证计划
- 步骤 1：前端构建：`frontend` 目录 `npm run build` 通过。
- 步骤 2：后端编译：`mvn "-DskipTests" compile` 通过。
- 步骤 3：Observability 验收：
  - 先在 Playground 触发一次同步运行（`/api/agent/run`），再进入 Observability；
  - Observability 能显示该次运行摘要与 steps 列表；
  - 导出能力可用（复制 JSON 或下载文件）。
- 步骤 4：README 验收：
  - README 中包含定位、能力地图、North Star Demo、运行方式、模块边界与讲解要点；
  - 按 README 步骤能跑通一次 Demo（至少到“Playground 同步运行 + Observability 查看 steps”）。
- 步骤 5：演示脚本验收：
  - 演示脚本文档存在，且完整覆盖 3~5 分钟节奏。

## 六、风险与假设
- 风险：若 Playground 不保存最近一次运行结果，则 Observability 需要先补“运行结果缓存/持久化”机制（建议前端 localStorage/内存持有，避免后端大改）。
- 假设：Sprint 4 不追求“全链路观测”，只要能提供“可复现证据 + 可讲解叙事”即可达标。

## 七、Definition of Done
- [ ] 合同中的核心用户行为已实现
- [ ] `frontend` 构建通过
- [ ] `mvn "-DskipTests" compile` 通过
- [ ] Observability 能展示并导出最近一次运行 steps
- [ ] README 与演示脚本可支撑 3~5 分钟面试讲解
- [ ] 本轮排除项未被误做进来
- [ ] 已知问题记录在 `artifacts/build_report.md`
