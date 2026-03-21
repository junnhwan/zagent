# SQL 使用说明

本目录只保留两个日常主入口脚本：

- `zagent_mysql.sql`：MySQL 主库表结构 + 基础示例数据
- `zagent_pgvector.sql`：PgVector / RAG 相关表结构

历史上的 MCP 场景脚本、修复脚本已归档到 `legacy/`，仅用于参考和追溯，不再作为日常联调主流程。

## 一、初始化顺序

### 1. 初始化 MySQL

执行：`docs/sql/zagent_mysql.sql`

作用：

- 创建 Agent / Client / Model / Prompt / Advisor / MCP 配置相关主表
- 插入项目基础示例数据
- 让后端具备最小可运行能力

### 2. 初始化 PgVector

执行：`docs/sql/zagent_pgvector.sql`

作用：

- 创建向量库相关表结构
- 为 RAG 文档入库、检索提供存储能力

## 二、MCP 配置怎么切换

现在不推荐再手动执行 `mcp_*.sql` 切换场景。

推荐方式：

1. 修改 `src/main/resources/mcp-tools.json`
2. 重启后端
3. 应用启动时自动把 MCP 的 model / tool / binding 同步到 MySQL

也就是说：

- **数据库是运行时存储**
- **JSON 是当前推荐的 MCP 配置入口**

## 三、当前 MCP 场景说明

`src/main/resources/mcp-tools.json` 里当前已经包含以下场景：

- `2002` -> `5001`：`stdio filesystem`
- `2003` -> `5002`：`sse probe`
- `2005` -> `5003`：高德 SSE（天气 + POI）

当前默认绑定：

- `3006` -> `2005`
- `3008` -> `2005`

如果要切换：

- 改 `client` 对应 binding 的 `targetIds`
- 重启后端即可

## 四、联调建议流程

### 1. 后端基础能力联调

先确认：

- MySQL 已初始化
- PgVector 已初始化
- Spring Boot 能正常启动

### 2. RAG 联调

准备知识文档后：

- 上传：`POST /api/rag/upload`
- 查询：`POST /api/rag/query`
- 标签列表：`GET /api/rag/tags`

### 3. Chat 联调

- 同步：`POST /api/chat`
- 流式：`GET /api/chat/stream`

### 4. Agent 联调

- 同步：`POST /api/agent/run`
- 流式：`GET /api/agent/run/stream`

### 5. MCP 联调

按需要启动对应外部 MCP：

- `stdio filesystem`：依赖本机 `npx`
- `sse probe`：运行 `python tools/mcp_sse_probe.py`
- `amap sse`：运行 `python tools/amap_sse_mcp.py`

然后修改 `mcp-tools.json` 绑定并重启后端。

## 五、legacy 目录是干嘛的

`docs/sql/legacy/` 中的脚本主要是：

- 历史联调阶段的快速切换脚本
- 某次问题修复时留下的补丁脚本
- 项目演进过程中的旧配置方式

现在一般不需要再执行这些脚本，除非你要复盘历史配置。

