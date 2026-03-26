# Build Report

## 文档状态
- 对应 Sprint：Sprint 4 - Observability 与简历化收尾
- 最后更新时间：2026-03-27
- 结论：已完成实现与自测，待归档

## 一、实现结论
- Observability 已从占位说明升级为可演示证据面板，可展示最近一次 Playground 同步运行的摘要与 steps。
- 运行态聚合已接入现有接口：MCP runtime status 与 RAG tags 概览。
- README 与演示脚本已重写，可直接支撑 3~5 分钟面试讲解。

## 二、已实现内容
- 运行证据落地
  - Playground 同步运行成功后写入 `localStorage`，固定 key 为 `za.lastRun`。
  - 存储结构包含：`lens`、`agentId`、`agentName`、`finalOutput`、`steps`、`createdAt`。
- Observability 页面增强
  - 展示最近一次运行摘要：范式视角、Agent、步骤数、记录时间、最终输出摘要。
  - 展示 steps 明细：`sequence/clientId/input/output`。
  - 支持复制观测 JSON 与下载观测文件。
  - 聚合展示 MCP runtime status 与 RAG tags 概览。
  - 缺失记录时显示“未发现运行记录 + 引导去 Playground”。
  - MCP/RAG 请求失败时显示“不可用：请求失败”，不阻塞核心验收。
- 文档交付
  - `README.md` 已重写，包含定位、能力地图、North Star Demo、运行方式、模块边界、讲解要点。
  - `docs/demo_script.md` 已更新，覆盖 3~5 分钟演示节奏与常见追问。

## 三、自测与验证
- 前端构建验证：
  - `Set-Location frontend; npm run build` 通过。
- 后端编译验证：
  - `mvn "-DskipTests" compile` 通过。
- 构建产物处理：
  - 已清理 `frontend/dist`。

## 四、已知问题
- 前端构建仍有 chunk size warning（历史问题，非本轮新增）。
- 本轮 Observability 以“最近一次运行证据 + 运行态聚合”为主，不包含完整 trace/tool-call 后端链路。

## 五、补充变更：MCP 配置迁移为纯 YAML
- MCP 主配置入口已统一迁移到 `src/main/resources/application.yml` 的 `zagent.mcp.sync.manifest`。
- 已删除 `src/main/resources/mcp-tools.json` 与 `src/test/resources/mcp-tools-test.json`，运行时不再依赖 JSON 文件。
- 新增 `McpSyncProperties` 与 `McpManifestStateHolder`，启动时从 YAML 装载 manifest，运行时模式切换仅影响当前进程内存态。
- `McpConfigSyncServiceImpl`、`McpModeAdminServiceImpl` 与对应测试已同步改造，文档也已统一更新到 YAML 口径。
- 本轮补充验证：
  - `mvn "-DskipTests" compile` 通过。
  - `mvn "-Dtest=McpConfigSyncServiceImplTest,McpModeAdminServiceImplTest" test` 通过。
