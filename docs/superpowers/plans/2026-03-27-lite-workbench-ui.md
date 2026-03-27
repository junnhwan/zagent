# Lite Workbench UI Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前偏“演示叙事”的前端壳层收敛为朴素、清晰的轻工作台。

**Architecture:** 维持现有 Vue 3 + Vite + Element Plus 结构，不改后端接口。通过调整主导航、Overview、Playground 和配置入口文案完成主体验收敛；复用现有 `za.lastRun`、MCP runtime status、RAG tags 数据作为首页和运行记录的数据来源。

**Tech Stack:** Vue 3、Vite、Element Plus、Axios、本地 `localStorage`

---

### Task 1: 沉淀轻工作台首页

**Files:**
- Modify: `frontend/src/views/Overview.vue`
- Use: `frontend/src/utils/observability.js`
- Use: `frontend/src/api/index.js`

- [ ] 读取最近一次运行记录与轻量系统状态来源。
- [ ] 将首页改为“快捷操作 + 最近运行 + 系统状态 + 配置入口”布局。
- [ ] 移除讲解型文案，改成中性工作台文案。
- [ ] 构建前端验证首页无编译错误。

### Task 2: 精简运行台

**Files:**
- Modify: `frontend/src/views/Playground.vue`

- [ ] 删除范式选择、讲解文案和“演示提示”输入。
- [ ] 保留 Agent、最大轮次、输入框、运行按钮、最终输出、步骤列表。
- [ ] 保留上一轮的错误态 / 空结果态可视化修复。
- [ ] 调整文案为运行导向。

### Task 3: 收敛导航与周边页面

**Files:**
- Modify: `frontend/src/layout/MainLayout.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/SettingsHub.vue`
- Modify: `frontend/src/views/ObservabilityHub.vue`
- Modify: `frontend/src/views/KnowledgeHub.vue`
- Modify: `frontend/src/views/Workflows.vue`

- [ ] 将一级导航收敛到高频入口。
- [ ] 页头和页面标题统一改成中性工作台风格。
- [ ] 让 Settings / Observability / Knowledge 文案更偏实际用途。
- [ ] 保持原路由兼容。

### Task 4: 收敛全局视觉并验证

**Files:**
- Modify: `frontend/src/styles/global.css`

- [ ] 减弱渐变、阴影和过强 Hero 视觉。
- [ ] 保持现有卡片体系和响应式布局。
- [ ] 运行 `Set-Location frontend; npm run build`。
- [ ] 清理构建产物并提交中文 commit。
