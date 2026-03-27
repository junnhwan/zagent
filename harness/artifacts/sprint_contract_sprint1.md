# Sprint Contract（归档）

## 文档状态
- 状态：已归档
- 对应 Sprint：Sprint 1 - 产品壳与演示入口重构
- 最后更新时间：2026-03-26

> 说明：为了继续起草后续 Sprint，本文件保留 Sprint 1 合同原文作为归档证据。

## 一、Sprint 目标
- 将当前前端从传统管理后台风格重构为 `Agent Studio` 产品壳，建立清晰的演示入口与面向用户目标的信息架构。

## 二、Included Scope
- 重构主导航、页头与整体布局风格。
- 新增固定一级路由：`/overview`、`/playground`、`/workflows`、`/tools`、`/knowledge`、`/observability`、`/settings`。
- 新增 `Overview` 页面，明确项目定位、核心亮点与推荐演示路径。
- 新增 `Playground` 页面，统一承接聊天测试与 Agent 运行入口。
- 新增 `Workflows`、`Tools`、`Knowledge`、`Observability` 介绍型页面，用于承接后续 Sprint 能力。
- 新增 `Settings` 聚合页，集中跳转到现有配置型页面。
- 保留现有旧页面实现，通过新壳层入口继续可达。
- 修复本轮触达范围内的中文乱码文案。

## 三、Excluded Scope
- 不重构后端包结构。
- 不修改数据库结构。
- 不实现真实 Observability 后端链路。
- 不迁移前端技术栈，不替换 Vue / Vite / Element Plus。
- 不对全部旧页面逐页深度重做视觉改版。

## 四、用户可感知行为
- 用户应能够：进入首页后 30 秒内理解 `zagent` 是一个 Agent Studio，而不是管理后台。
- 用户应能够：从主导航进入 `Overview`、`Playground`、`Settings`。
- 用户应能够：从 `Playground` 快速到达聊天测试与 Agent 运行演示入口。
- 用户应能够：从 `Settings` 快速到达 Agent、Client、Model、API、Prompt、Advisor、MCP、RAG 等旧有配置能力入口。

## 五、验证计划
- 步骤 1：在 `frontend` 目录执行 `npm run build`，构建必须通过。
- 步骤 2：验证以下一级路由存在且可从主导航访问：`/overview`、`/playground`、`/workflows`、`/tools`、`/knowledge`、`/observability`、`/settings`。
- 步骤 3：验证 `Overview` 首屏包含以下最小叙事要素：一句话产品定位、三个核心亮点、一条推荐演示路径。
- 步骤 4：验证 `Playground` 至少提供两个可达入口：聊天测试入口、Agent 运行入口。
- 步骤 5：验证 `Settings` 中至少可到达以下旧页面入口：`/agents`、`/clients`、`/models`、`/apis`、`/prompts`、`/advisors`、`/mcps`、`/rag`。
- 步骤 6：检查新导航与新页面中文文案无乱码。

## 六、风险与假设
- 风险：现有视图较多，若统一重写会超出 Sprint 1 范围。
- 风险：历史中文编码问题可能影响体验评分，需要在本轮触达范围内修复。
- 假设：保留旧页面实现，仅通过新的壳层与信息架构重新组织入口。

## 七、Definition of Done
- [ ] 合同中的核心用户行为已实现
- [ ] `frontend` 构建通过
- [ ] 新一级路由全部可访问
- [ ] 旧页面入口按合同要求可达
- [ ] 新导航与新页面中文文案无乱码
- [ ] 本轮排除项未被误做进来
- [ ] 已知问题已在 `build_report.md` 明确记录

