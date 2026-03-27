# Contract Review（Evaluator）

## 结论（最新：MCP 接入层收口合同审查）
- 复审轮次：第 1 次
- 结论：**已批准（可开工）**
- 审查时间：2026-03-27
- 对应合同：`harness/artifacts/sprint_contract.md`（专项修复 - MCP 接入层收口与双传输并挂）

## 审查要点与证据（MCP 专项修复）
- 目标闭合：合同已明确“同一 client 同时挂 `filesystem(stdio)` 与 `amap(sse)`”，没有再保留模糊的“模式切换”范围。
- 风险闭合：合同已把 `5004 git-repo` 的退役清理写入 Included Scope 和验证计划，避免只改 manifest 不改数据库残留。
- 口径闭合：合同已把“简历描述与代码实现一致”纳入验收项，能直接约束 `docs/简历项目表述与面试介绍.md` 与 `docs/sql/README.md` 的修正。
- 验证闭合：已列出定向测试集与编译验证命令，具备可执行性与可验收性。

---

# Contract Review（Evaluator）

## 结论（最新：Sprint 4 合同审查）
- 复审轮次：第 2 次
- 结论：**已批准（可开工）**
- 审查时间：2026-03-26
- 对应合同：`harness/artifacts/sprint_contract.md`（Sprint 4 - Observability 与简历化收尾）

## 审查要点与证据（Sprint 4）
- Observability 口径闭合：已写死数据来源为前端 `localStorage`，固定 key `za.lastRun`、最小 schema 与回退行为，避免诱发超范围后端改动。
- 聚合口径闭合：已写死接口来源为 `mcpApi.runtimeStatus` 与 `ragApi.tags`，并定义失败回退（显示“不可用/请求失败”，不阻塞核心验收）。
- 文档口径闭合：README 固定为 `README.md`，演示脚本文档固定为 `docs/demo_script.md`。

---

## 结论（最新：Sprint 3 合同审查）
- 复审轮次：第 2 次
- 结论：**已批准**
- 审查时间：2026-03-26
- 对应合同：`harness/artifacts/sprint_contract.md`（Sprint 3 - 后端模块化单体重构）

## 审查要点与证据（Sprint 3）
- 验收边界：合同已补齐固定测试清单，口径闭合。
- 迁移范围：合同已补齐最小迁移清单，明确 Controller 与高频契约的迁移对象。
- 接口兼容：合同已补齐字段级 wire contract 抽查点，覆盖 `/api/agent/run` 的关键字段。
- 结论：Sprint 3 合同当前可执行、可验收、范围受控，可开工。

---

## 结论（Sprint 2 合同审查）
- 复审轮次：第 1 次
- 结论：**已批准（可开工）**
- 审查时间：2026-03-26
- 对应合同：`harness/artifacts/sprint_contract.md`（Sprint 2 - Agent 核心编排体验强化）

## 审查要点与证据（Sprint 2）
- 可执行性：目标聚焦在 `Playground/Workflows` 的“范式视角 + steps 可见化 + 最终输出”，不依赖后端大改。
- 可验收性：验证计划给出构建命令、可见状态、steps 字段要求、可复制能力、跳转预选视角等明确检查项。
- 范围受控：Excluded Scope 明确不做后端模块化、DB 变更、Settings 全量重做、真实 Observability 链路。
- 协作可读性：已将 `harness/artifacts/*.md` 与 `harness/context/*.md` 统一为 UTF-8（含 BOM）编码，避免 PowerShell 默认读取乱码导致的“合同不同步”问题。

---

## 结论（Sprint 1 归档审查）
- 复审轮次：第 3 次（最终同步）
- 结论：**已批准**
- 审查时间：2026-03-26
- 对应合同（Sprint 1 归档）：`harness/artifacts/sprint_contract_sprint1.md`
- 关联最终验收：`harness/artifacts/qa_report.md`

## 审查要点与证据（Sprint 1）
- 合同可读性：Sprint 1 合同已以 UTF-8 中文可读形式归档（`harness/artifacts/sprint_contract_sprint1.md`）。
- 可验收标准：包含固定一级路由、旧页面入口清单、构建验证、Overview 首屏叙事最小要素（`harness/artifacts/sprint_contract_sprint1.md`）。
- 验收结果：已按合同与评分维度完成最终 QA，并给出 `PASS`（`harness/artifacts/qa_report.md`）。

## 备注（流程偏差说明）
- 在 Sprint 1 早期曾出现合同编码/同步问题，导致“先实现后批准”的偏差；本次第 3 次复审已基于工作区实际文件完成对齐，并完成最终批准与验收归档。
