# Sprint Contract

## 文档状态
- 状态：已批准
- 对应 Sprint：专项修复 - MCP 接入层收口与双传输并挂
- 最后更新时间：2026-03-27

> 说明：Sprint 4 合同已归档至 `artifacts/sprint_contract_sprint4.md`。

## 一、Sprint 目标
- 将当前 MCP 工具接入层收口到可稳定运行、可测试验证、可与简历表述对齐的状态。
- 让同一个 `ChatClient` 同时挂载 `filesystem(stdio)` 与 `amap(sse)` 两个 MCP 工具，并移除当前不需要的历史 MCP 场景与误导性模式切换语义。

## 二、Included Scope
- 后端 MCP 运行时收口
  - `3006`、`3008` 两个 MCP 相关 client 必须统一绑定到同一个 MCP model。
  - 该 MCP model 必须同时绑定 `5001 filesystem-docs(stdio)` 与 `5003 amap-sse(sse)`。
  - `5004 git-repo` 不再作为运行时可用 MCP，不再出现在当前 manifest 的有效配置中。
- 配置同步与清理
  - `src/main/resources/application.yml` 的 `zagent.mcp.sync.manifest` 必须成为当前唯一 MCP 默认配置真源。
  - 启动同步后，历史退役的 MCP model / MCP tool / 绑定关系必须被清理，避免管理页与运行态继续看到 `git-repo` 残留。
- 管理状态口径收口
  - `McpModeAdminServiceImpl` 保留接口兼容，但当前语义必须收口为单一 `bundle` 状态，不再保留 `stdio/amap` 二选一模式切换。
  - `getCurrentStatus()` 返回的 `activeMcps` 必须同时包含 `filesystem-docs` 与 `amap-sse` 两项。
- 测试与验证
  - 补充并更新后端单测，覆盖：双 MCP 绑定解析、manifest 同步清理、bundle 状态输出。
  - 至少跑通受影响测试集与后端编译验证。
- 文档与简历口径校准
  - `docs/简历项目表述与面试介绍.md` 必须更新为“数据库持久化关系 + manifest 同步默认 MCP 配置”的准确表述。
  - `docs/sql/README.md` 必须更新为“当前只保留 filesystem + amap 双工具并挂”的运行口径。

## 三、Excluded Scope
- 不新增新的 MCP 工具类型。
- 不做前端大改，只允许修正文案或运行口径相关说明。
- 不重做整套 Admin 管理体系。
- 不引入新的数据库表或新的配置中心。

## 四、用户可感知行为
- 用户应能够：在配置与运行时只看到两个 MCP 工具：`filesystem-docs` 与 `amap-sse`。
- 用户应能够：同一个 Agent 运行中同时使用 `stdio filesystem` 与 `sse amap` 两种 MCP 能力。
- 用户应能够：按当前实现与文档描述，自圆其说地说明项目“支持 `stdio / sse` 双传输，并统一转为 Spring AI Tool 能力”。

## 五、验证计划
- 步骤 1：失败测试
  - 修改 `McpBindingResolverImplTest`，期望同一 model 返回 `5001 + 5003`；
  - 修改 `McpModeAdminServiceImplTest`，期望 `bundle` 状态下 `activeMcps` 为两项；
  - 新增/修改 `McpConfigSyncServiceImplTest`，验证退役 MCP 在同步后被清理。
- 步骤 2：实现后执行定向测试
  - `mvn "-Dtest=AiClientAssemblyServiceImplTest,McpBindingResolverImplTest,McpConfigSyncServiceImplTest,McpModeAdminServiceImplTest" test`
- 步骤 3：编译验证
  - `mvn "-DskipTests" compile`
- 步骤 4：口径复核
  - 对照 `docs/简历项目表述与面试介绍.md`、`docs/sql/README.md` 与实际代码确认表述一致。

## 六、风险与假设
- 风险：如果只改 manifest 而不做清理逻辑，`git-repo` 可能仍残留在数据库和管理页，导致“代码口径”与“界面口径”不一致。
- 风险：如果完全删除模式接口，可能影响历史兼容；因此本轮采取“接口保留、语义收口”为优先方案。
- 假设：`3006` 与 `3008` 是当前受 MCP 管理的核心 client，其他 client 不作为本轮 MCP 收口对象。

## 七、Definition of Done
- [ ] `filesystem-docs(stdio)` 与 `amap-sse(sse)` 可同时成为同一 MCP model 的可用工具
- [ ] `git-repo` 不再作为当前运行时有效 MCP 出现
- [ ] `McpBindingResolverImplTest`、`McpConfigSyncServiceImplTest`、`McpModeAdminServiceImplTest` 通过
- [ ] `mvn "-DskipTests" compile` 通过
- [ ] 简历与运行文档口径与真实实现一致
- [ ] 已知问题记录到 `artifacts/build_report.md`
