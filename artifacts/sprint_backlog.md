# Sprint Backlog

## 使用说明
- 由 `Planner` 维护
- 每个 Sprint 必须清楚写明：目标、范围、排除项、依赖、验收关注点
- Sprint 粒度应支持 `Generator` 在单轮内完成一个有意义、可验证的增量

---

## Sprint 1
### 目标
- 将项目前端从传统管理后台重构为面向演示的 `Agent Studio` 产品壳。

### Included Scope
- 重写一级导航与页面分组。
- 新增 `Overview`、`Playground`、`Workflows`、`Tools`、`Knowledge`、`Observability`、`Settings` 主入口。
- 保留现有配置与测试能力，通过二级入口或分组卡片承接旧页面。
- 统一视觉语言、首页叙事与主操作入口。

### Excluded Scope
- 不做后端包结构重构。
- 不做 Observability 的真实后端数据链路。
- 不做全量页面逐页精修。

### 依赖
- 现有 Vue 3 + Vite + Element Plus 工程可继续使用。

### 验收关注点
- 第一眼不再像传统后台。
- 用户能快速找到演示入口。
- 已有核心页面仍可通过新壳层访问。
- 导航与页面中文文案不得出现乱码。
- North Star Demo 路径成立：`Overview -> Playground -> (触发一次演示运行入口)`。

---

## Sprint 2
### 目标
- 强化 Agent 编排体验，提升策略范式的可讲解性。

### Included Scope
- 在 `Playground/Workflows` 中展示策略差异与推荐使用场景（面向讲解而非配置堆叠）。
- 将一次 Agent 运行结果拆分为“最终输出 + 步骤列表”，并以时间线/卡片形式可视化（基于后端返回的 `AgentResultVO.steps`）。
- 让用户能显式选择/识别三类范式视角：`Plan and Execute`、`ReAct`、`Reflection`（允许先做“讲解视角 + 映射到现有策略实现”的落地方式）。
- 为演示提供最短闭环：一次运行至少展示 3 个关键要素：选择的范式/策略、步骤化过程、最终输出。
- 可选增强（不强制）：接入 `/api/agent/run/stream` 的 SSE 流式演示入口。

### Excluded Scope
- 不做后端模块化迁移。
- 不重做全部设置页。
- 不引入数据库结构变更。

### 依赖
- 依赖 Sprint 1 的新信息架构。

### 验收关注点
- 能清楚讲明 `Plan and Execute`、`ReAct`、`Reflection` 的差异与入口。
- 演示中能解释“Prompt/Tool/RAG/Memory（如有）各自作用”，并指向对应入口。
- 演示中能展示“步骤化过程”，而不是只展示最终输出。

---

## Sprint 3
### 目标
- 将后端目录从“技术层混放”收敛为“领域模块 + 模块内分层”的模块化单体结构，并保证对外 API 行为稳定。

### Included Scope
- 按领域模块（`agent/tooling/knowledge/runtime/observability/configcenter/web`）建立可讲解的包结构骨架（模块化单体，不拆 Maven 多模块）。
- 控制器入口按领域归位，形成统一 `web` 入口层（Controller/Request/Response），并保持对外 API 路径不变。
- 收缩共享模型：将纯 web 契约（DTO/VO）从通用 `model/*` 中剥离，迁移到更贴近 `web` 或领域模块的包中。
- 最小迁移：以“移动包 + 调整 Spring/MyBatis 扫描 + 修复引用”为主，不引入新业务能力。
- 执行回归验证：`mvn "-DskipTests" compile` 必须通过，受影响测试集必须通过。

### Excluded Scope
- 不拆微服务。
- 不强制升级为 Maven 多模块。
- 不修改数据库结构与存量数据。
- 不变更对外 API 路径/请求体/响应体（除非属于修复 bug 且有明确兼容策略）。
- 不做业务逻辑层面的全面重写。

### 依赖
- 依赖 Sprint 1/2 已稳定的页面与演示路径。

### 验收关注点
- 目录结构清晰。
- 后端职责边界可解释。
- 回归测试通过。
- 原有前端依赖的 API 路径保持兼容。
- 关键 API（/api/agent、/api/chat、/api/rag、/api/admin 等）行为不回退（至少保持可编译、可启动、接口签名稳定）。

---

## Sprint 4
### 目标
- 完成可观测性、README 与简历化叙事收尾。

### Included Scope
- Observability 页面增强：至少能展示一次运行的 step/tool-call 记录（读取真实数据或可复现的记录源）。
- README 重写：用“产品叙事 + 能力地图 + 演示路径 + 架构图/模块边界”组织内容。
- 演示脚本与项目亮点整理：按 3~5 分钟演示节奏编排。

### Excluded Scope
- 不新增与主线无关功能。

### 依赖
- 依赖前 3 个 Sprint 的稳定实现。

### 验收关注点
- 适合对外演示。
- 适合简历与面试表达。
