<template>
  <div class="page-shell">
    <section class="hero-panel compact">
      <div>
        <p class="eyebrow">Settings</p>
        <h1>配置</h1>
        <p class="hero-copy">模型、Prompt、MCP、Agent 和知识库等运行配置统一放在这里。</p>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>配置入口</h3>
        <p>按配置项分类进入即可。</p>
      </div>
      <div class="card-grid cols-3">
        <article class="info-card link-card" v-for="item in settingsGroups" :key="item.path" @click="$router.push(item.path)">
          <div class="link-row">
            <h4>{{ item.title }}</h4>
            <span>{{ item.badge }}</span>
          </div>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
const settingsGroups = [
  { title: 'Agent', desc: '管理 Agent、流程和运行入口配置。', path: '/settings/agents', badge: '/agents' },
  { title: 'Client', desc: '管理 AI Client 与绑定关系。', path: '/settings/clients', badge: '/clients' },
  { title: 'Model', desc: '管理模型列表与可用能力。', path: '/settings/models', badge: '/models' },
  { title: 'API', desc: '管理模型服务 API 与调用地址。', path: '/settings/apis', badge: '/apis' },
  { title: 'Prompt', desc: '管理系统 Prompt 与指令模板。', path: '/settings/prompts', badge: '/prompts' },
  { title: 'Advisor', desc: '管理记忆和增强组件。', path: '/settings/advisors', badge: '/advisors' },
  { title: 'MCP', desc: '管理 MCP 服务与工具接入。', path: '/settings/mcps', badge: '/mcps' },
  { title: 'Knowledge', desc: '管理知识库与 RAG 标签。', path: '/rag', badge: '/rag' }
]
</script>
