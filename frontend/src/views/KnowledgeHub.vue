<template>
  <div class="page-shell">
    <section class="hero-panel compact">
      <div>
        <p class="eyebrow">Knowledge</p>
        <h1>知识库</h1>
        <p class="hero-copy">在这里上传文档、管理标签，并使用现有检索能力做测试。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="$router.push('/rag')">打开知识库</el-button>
        <el-button @click="$router.push('/settings')">打开配置</el-button>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>当前入口</h3>
        <p>常用知识库操作可以从这里进入。</p>
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
  { title: '文档上传', desc: '通过现有知识库管理页上传文档。' },
  { title: '标签管理', desc: '按标签区分不同知识集合。' },
  { title: '检索测试', desc: '使用现有查询入口验证检索效果。' }
]
</script>
