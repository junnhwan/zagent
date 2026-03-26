<template>
  <div class="page-shell">
    <section class="hero-panel compact">
      <div>
        <p class="eyebrow">Observability</p>
        <h1>先把观测面立住，再逐步接入真实运行数据</h1>
        <p class="hero-copy">
          Sprint 1 先提供讲解框架，明确后续将展示 trace、step、tool call、RAG 命中与运行状态。
        </p>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>后续观测对象</h3>
        <p>本轮先让用户知道系统会观测什么，而不是只看到最终回答。</p>
      </div>
      <div class="card-grid cols-2">
        <article class="info-card" v-for="item in observabilityItems" :key="item.title">
          <h4>{{ item.title }}</h4>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
const observabilityItems = [
  { title: 'Trace / Step', desc: '展示一次 Agent 运行中的阶段拆分、思考步骤与状态变化。' },
  { title: 'Tool Call', desc: '展示调用了哪些工具、参数是什么、结果是否成功。' },
  { title: 'RAG 命中', desc: '展示知识检索是否发生、命中了哪些文档标签。' },
  { title: '运行状态', desc: '展示 MCP 服务、缓存与运行时资源状态，辅助诊断。' }
]
</script>
