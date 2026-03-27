# AGENTS.md

## 目标
本目录实现一个严格参考 Anthropic 三代理 Harness 思路的工作流：
- `Planner` 负责扩展产品规格与 Sprint 拆分
- `Generator` 负责按合同实施 Sprint
- `Evaluator` 负责合同审查与独立验收

## 目录约定
- Harness 模板相关目录统一收敛在 `harness/` 下。
- 代理说明位于 `harness/agents/`。
- 上下文、工件、日志、脚本分别位于 `harness/context/`、`harness/artifacts/`、`harness/logs/`、`harness/scripts/`。

## 启动顺序
每次新任务开始时，按以下顺序读取：
1. `harness/context/product_request.md`
2. `harness/context/project_context.md`
3. `harness/artifacts/product_spec.md`（如果已存在）
4. `harness/artifacts/sprint_backlog.md`（如果已存在）
5. `harness/artifacts/sprint_contract.md`（如果当前 Sprint 已起草）
6. `harness/artifacts/eval_criteria.md`

## 协作协议
- 所有代理都必须通过文件工件协作，不依赖“上一轮聊天里说过什么”。
- 任何 Sprint 在编码前，必须先生成并批准 `harness/artifacts/sprint_contract.md`。
- `Planner` 不写代码，只写高层产品与分 Sprint 设计。
- `Generator` 不跳过合同，不偷加范围，不跳过验证。
- `Evaluator` 必须独立、怀疑式、证据驱动，不因“看起来不错”而放宽标准。

## 验收门禁
- `Evaluator` 必须按照 `harness/artifacts/eval_criteria.md` 逐项评分。
- 任一项分数低于阈值，则本轮 Sprint 结论为 `FAIL`。
- `Generator` 只有在 `harness/artifacts/qa_report.md` 为 `PASS` 时，才能推进到下一个 Sprint。

## 工件维护规则
- `harness/artifacts/product_spec.md`：由 `Planner` 维护，高层规格，避免过早写死实现细节。
- `harness/artifacts/sprint_backlog.md`：由 `Planner` 维护，记录 Sprint 顺序、目标、依赖。
- `harness/artifacts/sprint_contract.md`：由 `Generator` 起草，由 `Evaluator` 审核。
- `harness/artifacts/contract_review.md`：由 `Evaluator` 维护，记录合同审查意见。
- `harness/artifacts/build_report.md`：由 `Generator` 维护，记录本轮实现、自测、已知问题。
- `harness/artifacts/qa_report.md`：由 `Evaluator` 维护，记录最终 QA 结论。
- `harness/artifacts/handoff.md`：在长任务切换阶段时维护，便于重启或切换会话。

## 输出风格
- 一律使用中文。
- 先给结论，再给证据。
- 评分、结论、阻塞项必须明确。
- 不写空话，不写“基本完成”“应该可以”等模糊表述。

## 长任务建议
- 优先持续会话 + 工件沉淀。
- 如果上下文开始漂移，可根据 `harness/artifacts/handoff.md` 开新会话继续。
- 开新会话时，不依赖旧对话，必须重新读取工件文件。
