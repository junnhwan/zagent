<template>
  <div class="page-shell">
    <section class="hero-panel compact">
      <div>
        <p class="eyebrow">Knowledge</p>
        <h1>知识增强能力是 Agent 输出质量的重要支撑</h1>
        <p class="hero-copy">
          当前版本通过现有 RAG 页面承接文档上传、标签管理与检索测试；后续会补齐命中证据、召回链路与结果解释能力。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="$router.push('/rag')">打开知识库</el-button>
        <el-button @click="$router.push('/settings')">查看配置入口</el-button>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>当前可演示内容</h3>
        <p>先把知识库入口立住，再在后续 Sprint 补真实命中可视化。</p>
      </div>
      <div class="card-grid cols-3">
        <article class="info-card" v-for="item in knowledgeItems" :key="item.title">
          <h4>{{ item.title }}</h4>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
const knowledgeItems = [
  { title: '文档上传', desc: '通过现有 RAG 管理页上传知识文档，建立基础语料。' },
  { title: '标签隔离', desc: '按标签管理知识集合，支持面向场景的演示和清理。' },
  { title: '检索问答', desc: '后续将在 Playground 与 Observability 中展示命中证据与引用来源。' }
]
</script>
