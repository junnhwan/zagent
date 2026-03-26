<template>
  <div class="page-shell">
    <section class="hero-panel compact">
      <div>
        <p class="eyebrow">Settings</p>
        <h1>配置能力统一收口到 Settings，不再挤占主叙事</h1>
        <p class="hero-copy">
          这些页面仍然重要，但它们是支撑能力，不再作为用户进入系统后的第一视觉焦点。
        </p>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>配置入口</h3>
        <p>以下入口承接 Sprint 1 合同要求的旧页面可达性。</p>
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
  { title: 'Agent', desc: '管理 Agent、流程与运行入口配置。', path: '/settings/agents', badge: '/agents' },
  { title: 'Client', desc: '管理 AI Client 装配与绑定关系。', path: '/settings/clients', badge: '/clients' },
  { title: 'Model', desc: '管理模型列表与可用能力。', path: '/settings/models', badge: '/models' },
  { title: 'API', desc: '管理模型服务 API 与调用地址。', path: '/settings/apis', badge: '/apis' },
  { title: 'Prompt', desc: '管理系统 Prompt 与指令模板。', path: '/settings/prompts', badge: '/prompts' },
  { title: 'Advisor', desc: '管理记忆、顾问与增强组件。', path: '/settings/advisors', badge: '/advisors' },
  { title: 'MCP', desc: '管理 MCP 服务与工具接入。', path: '/settings/mcps', badge: '/mcps' },
  { title: 'Knowledge', desc: '管理知识库与 RAG 标签。', path: '/rag', badge: '/rag' }
]
</script>
