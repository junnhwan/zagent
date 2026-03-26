# zagent

`zagent` 是一个面向演示、学习与简历展示的 **Agent Studio**，用于集中展示：
- Agent 编排范式（Plan and Execute / ReAct / Reflection）
- Prompt Engineering 与 Tool Use
- RAG 知识增强
- 运行过程可观测性（Observability）

## 1. 一句话定位
- 一个可配置、可运行、可观测、可讲解的 Agent 编排实验室。

## 2. 能力地图
- **Plan and Execute**：先规划再执行，适合复杂任务分解。
- **ReAct**：边思考边行动，适合工具调用闭环。
- **Reflection**：先产出后复盘，适合结果质量优化。
- **Tool Use**：通过 MCP 工具接入外部能力。
- **RAG**：通过知识库与标签机制增强回答质量。
- **Observability**：展示最近一次运行证据（摘要 + steps + 运行态聚合）。

## 3. North Star Demo（3~5 分钟）
1. 进入 `Overview`：解释项目定位与能力边界。
2. 进入 `Workflows`：讲清三种编排范式差异。
3. 进入 `Playground`：选择范式、运行一次同步任务。
4. 进入 `Observability`：查看最近一次运行摘要、steps 证据、MCP/RAG 聚合信息。
5. 最后进入 `Settings`：说明配置能力是支撑层，而不是主叙事。

## 4. 本地运行方式

### 4.1 后端（Spring Boot）
```bash
mvn "-DskipTests" compile
mvn spring-boot:run
```

### 4.2 前端（Vue 3 + Vite）
```bash
cd frontend
npm install
npm run dev
```

### 4.3 验证命令
```bash
# 后端编译
mvn "-DskipTests" compile

# 前端构建
cd frontend && npm run build
```

## 5. 后端模块边界（模块化单体）
- `agent`：Agent 执行编排、策略与运行契约。
- `tooling`：工具注册、MCP 绑定与调用抽象。
- `knowledge`：RAG、知识标签与检索能力。
- `runtime`：运行态上下文与中间产物。
- `observability`：运行证据聚合与可观测能力（持续增强）。
- `configcenter`：模型、Prompt、API、Advisor、Client 等配置能力。
- `web`：对外 API 入口层（按领域归位的 Controller 与契约）。

## 6. 面试讲解要点
- 先讲“为什么做”：从传统后台转为 Agent Studio，强调可讲解与可验证。
- 再讲“怎么做”：Sprint 化推进，先立产品壳，再补可视化编排，再做后端结构收敛。
- 最后讲“证据”：
  - Playground 可运行；
  - Observability 可拿到 steps 证据并导出；
  - 后端完成模块化单体骨架重构且 API 兼容。

## 7. 相关文档
- Sprint 规划与实施工件：`artifacts/`
- 演示脚本：`docs/demo_script.md`
