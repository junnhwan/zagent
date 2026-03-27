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

## 六、补充变更：移除 sse_probe MCP
- 已从 `application.yml` 的 MCP manifest 中删除 `2003 -> 5002 -> sse-probe` 配置。
- 已从 `McpModeAdminServiceImpl` 的运行时模式列表中删除 `sse_probe`，当前仅保留 `stdio` 与 `amap` 两种可切换模式。
- 已删除 `tools/mcp_sse_probe.py`、对应 `__pycache__` 文件，以及 `docs/sql/legacy/` 下所有仅服务于 sse_probe 的历史脚本。
- MCP 文档与测试提示词已同步更新，不再引导使用已删除的 sse_probe。
- 本轮补充验证：
  - `mvn "-Dtest=McpModeAdminServiceImplTest,McpBindingResolverImplTest" test` 通过。
  - `mvn "-DskipTests" compile` 通过。

## 七、补充变更：前端收敛为轻工作台
- 已将前端首页从“演示叙事页”收敛为轻工作台：首页展示快捷操作、最近一次运行、系统状态与常用配置入口。
- 已将 Playground 收敛为纯运行台：删除范式切换与演示提示，只保留 Agent、输入、输出和步骤结果。
- 已将主导航收敛为高频入口：工作台、运行台、知识库、运行记录、配置；`Workflows` 与 `Tools` 不再作为一级重点入口。
- 已统一收缩文案与视觉：去掉首页和主路径中的“演示 / 面试 / 讲解”表述，减弱渐变和过强阴影。
- 本轮补充验证：
  - `Set-Location frontend; npm run build` 通过。
  - `mvn "-DskipTests" compile` 通过。
- 已补充同步运行超时修复：前端 `agentRunApi.run` 不再设置固定 120 秒超时，避免“后端仍在执行、前端先判失败”。
- 已补充后端同步运行耗时日志：`Agent同步执行开始/完成/失败`，便于定位响应何时真正返回。

## 八、补充变更：MCP SSE 兼容修复与前端状态纠偏
- 已新增 `tools/mcp_transport_compat.py`，统一承接 Python SSE MCP 服务的兼容层与日志降噪逻辑。
- `tools/amap_sse_mcp.py` 与 `tools/git_repo_mcp.py` 已接入同一套兼容中间件：
  - 缺失 `Content-Type` 时自动补为 `application/json`，并降为 debug 级别日志。
  - 对 `/messages/` 的空 POST 请求返回 `202 Accepted`，避免 FastMCP 因空 JSON 直接报 `EOF` / `400`。
  - 对 uvicorn 的 `Unsupported upgrade request` / `No supported WebSocket library detected` 噪音告警加过滤，控制台更干净。
- 已新增回归测试 `tools/test_mcp_transport_compat.py`，覆盖：
  - 缺失 `Content-Type` 的兼容补齐；
  - 空 POST 请求的容错返回；
  - uvicorn 升级噪音日志过滤。
- 已新增前端工具 `frontend/src/utils/mcpRuntime.js`，把后端返回的 `initialized/ready` 统一归一成前端使用的 `connected`。
- `frontend/src/views/Overview.vue`、`frontend/src/views/ObservabilityHub.vue`、`frontend/src/views/McpManage.vue` 已接入归一化逻辑，修复“后端成功但前端仍显示未连接/0/N”的误判。
- 本轮补充验证：
  - `python` 单测：`tools.test_amap_sse_mcp`、`tools.test_mcp_transport_compat` 通过。
  - `mvn "-DskipTests" compile` 通过。
  - `Set-Location frontend; npm run build` 通过。
