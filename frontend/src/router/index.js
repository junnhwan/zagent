import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/overview',
    children: [
      {
        path: 'overview',
        name: 'Overview',
        component: () => import('../views/Overview.vue'),
        meta: { title: 'Overview' }
      },
      {
        path: 'playground',
        name: 'Playground',
        component: () => import('../views/Playground.vue'),
        meta: { title: 'Playground' }
      },
      {
        path: 'workflows',
        name: 'Workflows',
        component: () => import('../views/Workflows.vue'),
        meta: { title: 'Workflows' }
      },
      {
        path: 'tools',
        name: 'ToolsHub',
        component: () => import('../views/ToolsHub.vue'),
        meta: { title: 'Tools' }
      },
      {
        path: 'knowledge',
        name: 'KnowledgeHub',
        component: () => import('../views/KnowledgeHub.vue'),
        meta: { title: 'Knowledge' }
      },
      {
        path: 'observability',
        name: 'ObservabilityHub',
        component: () => import('../views/ObservabilityHub.vue'),
        meta: { title: 'Observability' }
      },
      {
        path: 'settings',
        name: 'SettingsHub',
        component: () => import('../views/SettingsHub.vue'),
        meta: { title: 'Settings' }
      },
      {
        path: 'settings/agents',
        name: 'AgentManage',
        component: () => import('../views/AgentManage.vue'),
        meta: { title: 'Agent 配置' }
      },
      {
        path: 'settings/clients',
        name: 'ClientManage',
        component: () => import('../views/ClientManage.vue'),
        meta: { title: 'Client 配置' }
      },
      {
        path: 'settings/models',
        name: 'ModelManage',
        component: () => import('../views/ModelManage.vue'),
        meta: { title: '模型配置' }
      },
      {
        path: 'settings/apis',
        name: 'ApiManage',
        component: () => import('../views/ApiManage.vue'),
        meta: { title: 'API 配置' }
      },
      {
        path: 'settings/prompts',
        name: 'PromptManage',
        component: () => import('../views/PromptManage.vue'),
        meta: { title: 'Prompt 配置' }
      },
      {
        path: 'settings/advisors',
        name: 'AdvisorManage',
        component: () => import('../views/AdvisorManage.vue'),
        meta: { title: 'Advisor 配置' }
      },
      {
        path: 'settings/mcps',
        name: 'McpManage',
        component: () => import('../views/McpManage.vue'),
        meta: { title: 'MCP 配置' }
      },
      {
        path: 'chat',
        name: 'ChatTest',
        component: () => import('../views/ChatTest.vue'),
        meta: { title: 'Chat Playground' }
      },
      {
        path: 'agent-test',
        name: 'AgentTest',
        component: () => import('../views/AgentTest.vue'),
        meta: { title: 'Agent Playground' }
      },
      {
        path: 'rag',
        name: 'RagManage',
        component: () => import('../views/RagManage.vue'),
        meta: { title: 'Knowledge Base' }
      }
    ]
  },
  { path: '/dashboard', redirect: '/overview' },
  { path: '/agents', redirect: '/settings/agents' },
  { path: '/clients', redirect: '/settings/clients' },
  { path: '/models', redirect: '/settings/models' },
  { path: '/apis', redirect: '/settings/apis' },
  { path: '/prompts', redirect: '/settings/prompts' },
  { path: '/advisors', redirect: '/settings/advisors' },
  { path: '/mcps', redirect: '/settings/mcps' },
  { path: '/:pathMatch(.*)*', redirect: '/overview' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - zagent`
  }
  next()
})

export default router
