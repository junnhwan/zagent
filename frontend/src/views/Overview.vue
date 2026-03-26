<template>
  <div class="page">
    <section class="hero-card">
      <p class="hero-tag">Agent Studio</p>
      <h1>zagent：可配置、可观察、可演示的 Agent 编排实验室</h1>
      <p class="hero-desc">
        用统一入口展示 Plan and Execute、ReAct、Reflection，帮助你在演示与面试中清晰讲解 Agent 的设计与落地。
      </p>
      <div class="hero-actions">
        <el-button type="primary" @click="$router.push('/playground')">进入 Playground</el-button>
        <el-button @click="$router.push('/settings')">查看配置中心</el-button>
      </div>
    </section>

    <section class="section">
      <h3>核心能力</h3>
      <el-row :gutter="16">
        <el-col v-for="item in capabilities" :key="item.title" :md="8" :sm="12" :xs="24">
          <el-card class="card">
            <h4>{{ item.title }}</h4>
            <p>{{ item.desc }}</p>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <section class="section">
      <h3>推荐演示路径</h3>
      <el-steps :active="2" finish-status="success" simple>
        <el-step title="Overview" description="先解释项目定位与能力边界" />
        <el-step title="Playground" description="演示策略执行与结果输出" />
        <el-step title="Observability" description="补充运行过程与观测视角" />
      </el-steps>
    </section>
  </div>
</template>

<script setup>
const capabilities = [
  { title: 'Agent 编排', desc: '支持多策略入口，聚焦可讲解性和可演示性。' },
  { title: 'Tool Use', desc: '将工具调用从隐式配置提升为可见能力。' },
  { title: 'Knowledge + RAG', desc: '通过知识库能力扩展回答质量与场景覆盖。' },
  { title: 'Prompt Engineering', desc: '将 Prompt 作为可调节的关键参数进行管理。' },
  { title: '配置解耦', desc: '将运行配置归档到 Settings，避免干扰主流程。' },
  { title: '可观测性', desc: '为后续 Trace、步骤和运行诊断提供承接入口。' }
]
</script>
