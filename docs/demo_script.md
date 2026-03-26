# zagent 3~5 分钟演示脚本

## 一、演示目标（15 秒）
- 目标：在 3~5 分钟内让听众理解这是一个可讲解、可验证的 Agent Studio，而不是传统配置后台。
- 核心证据：`Playground` 可运行、`Observability` 可展示并导出 steps、后端边界可解释。

## 二、演示节奏

### 1）开场定位（30 秒）
- 话术：
  - “这是我做的 `zagent`，定位是一个 Agent Studio。”
  - “我重点展示三件事：编排范式、运行证据、工程结构。”

### 2）讲编排范式（45 秒）
- 页面：`Workflows`
- 话术：
  - “`Plan and Execute` 适合复杂任务分解。”
  - “`ReAct` 适合工具调用闭环。”
  - “`Reflection` 适合结果复盘与优化。”
- 操作：点击任一范式卡片“跳转演示”。

### 3）跑一次 Agent（60~90 秒）
- 页面：`Playground`
- 操作：
  - 选择 Agent
  - 输入任务
  - 点击“运行演示”
- 话术：
  - “这里不仅看最终输出，还看 steps 过程证据。”
  - “运行完成后会保存最近一次运行记录，用于 Observability 展示。”

### 4）展示 Observability（60 秒）
- 页面：`Observability`
- 操作：
  - 展示最近一次运行摘要
  - 展示 steps 明细
  - 点击“复制观测 JSON”或“下载观测文件”
  - 查看 MCP runtime status 与 RAG tags 概览
- 话术：
  - “我不只展示答案，还展示答案是如何产生的。”

### 5）讲工程结构（30~45 秒）
- 参考：`README.md` 的模块边界部分
- 话术：
  - “后端从技术层混放收敛到模块化单体骨架。”
  - “`agent` 负责编排，`web` 负责入口，`observability` 负责证据聚合。”

## 三、常见追问与回答

### Q1：为什么不直接上成熟 Agent 框架？
- 回答：
  - 我刻意保留可解释的工程边界，重点展示对编排范式、工具链路和运行证据的理解，而不是只会调用框架。

### Q2：为什么先做模块化单体，不直接拆微服务？
- 回答：
  - 当前阶段先保证“演示闭环 + 可维护边界”，模块化单体更稳，后续可按边界继续拆分。

### Q3：Observability 为什么先展示 steps？
- 回答：
  - 面试演示最需要可复现证据。steps 已经能说明运行过程，完整 trace/tool-call 链路是后续增强。

## 四、演示前检查清单
- 后端：`mvn "-DskipTests" compile` 通过
- 前端：`cd frontend && npm run build` 通过
- Playground 已跑过一次同步任务
- Observability 页面已有最近一次运行记录
