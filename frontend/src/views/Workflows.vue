<template>
  <div class="page-shell">
    <section class="hero-panel compact">
      <div>
        <p class="eyebrow">Workflows</p>
        <h1>工作流</h1>
        <p class="hero-copy">集中查看当前支持的三种策略说明和配置映射。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="$router.push('/playground')">打开运行台</el-button>
        <el-button @click="$router.push('/settings/agents')">查看 Agent 配置</el-button>
      </div>
    </section>

    <section class="section-block">
      <div class="card-grid cols-3">
        <article class="info-card workflow-card" v-for="mode in modes" :key="mode.label">
          <div class="workflow-head">
            <div>
              <h3>{{ mode.label }}</h3>
              <p class="workflow-tag">{{ mode.tag }}</p>
            </div>
            <el-tag type="primary" effect="plain">{{ mode.scene }}</el-tag>
          </div>

          <p class="workflow-desc">{{ mode.desc }}</p>

          <div class="workflow-meta">
            <div>
              <label>适用场景</label>
              <p>{{ mode.sceneDetail }}</p>
            </div>
            <div>
              <label>注意点</label>
              <p>{{ mode.risk }}</p>
            </div>
            <div>
              <label>策略映射</label>
              <p>{{ mode.mapping }}</p>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
const modes = [
  {
    label: 'Plan and Execute',
    tag: '规划优先',
    scene: '复杂任务',
    desc: '先规划再执行，适合任务拆解和多阶段推进。',
    sceneDetail: '适合行程规划、研究任务、复杂需求拆分等需要结构化步骤的任务。',
    risk: '如果规划过于理想化，执行阶段可能与真实工具能力脱节。',
    mapping: 'flow / plan_execute'
  },
  {
    label: 'ReAct',
    tag: '交互闭环',
    scene: '工具调用',
    desc: '边思考边行动，强调工具调用和观察反馈。',
    sceneDetail: '适合工具使用、信息探测、逐步收敛答案的场景。',
    risk: '若观察结果噪声较大，容易导致循环冗长或步骤不稳定。',
    mapping: 'react'
  },
  {
    label: 'Reflection',
    tag: '复盘优化',
    scene: '质量提升',
    desc: '先给出结果，再自我审查和修正。',
    sceneDetail: '适合写作、总结、计划输出等需要结果复盘的任务。',
    risk: '反思过程会增加耗时，如果没有明确标准可能出现过度修正。',
    mapping: 'auto'
  }
]
</script>

<style scoped>
.workflow-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workflow-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.workflow-head h3 {
  font-size: 22px;
  margin-bottom: 6px;
}

.workflow-tag {
  color: #64708b;
}

.workflow-desc {
  color: #55627b;
  line-height: 1.7;
}

.workflow-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.workflow-meta label {
  display: block;
  margin-bottom: 4px;
  font-weight: 600;
}

.workflow-meta p {
  color: #5f6b85;
  line-height: 1.65;
}
</style>
