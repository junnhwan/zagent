import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'agents',
        name: 'AgentManage',
        component: () => import('../views/AgentManage.vue'),
        meta: { title: 'Agent 管理' }
      },
      {
        path: 'clients',
        name: 'ClientManage',
        component: () => import('../views/ClientManage.vue'),
        meta: { title: 'Client 管理' }
      },
      {
        path: 'models',
        name: 'ModelManage',
        component: () => import('../views/ModelManage.vue'),
        meta: { title: '模型管理' }
      },
      {
        path: 'apis',
        name: 'ApiManage',
        component: () => import('../views/ApiManage.vue'),
        meta: { title: 'API 管理' }
      },
      {
        path: 'prompts',
        name: 'PromptManage',
        component: () => import('../views/PromptManage.vue'),
        meta: { title: '提示词管理' }
      },
      {
        path: 'advisors',
        name: 'AdvisorManage',
        component: () => import('../views/AdvisorManage.vue'),
        meta: { title: 'Advisor 管理' }
      },
      {
        path: 'mcps',
        name: 'McpManage',
        component: () => import('../views/McpManage.vue'),
        meta: { title: 'MCP 工具管理' }
      },
      {
        path: 'chat',
        name: 'ChatTest',
        component: () => import('../views/ChatTest.vue'),
        meta: { title: '对话测试' }
      },
      {
        path: 'agent-test',
        name: 'AgentTest',
        component: () => import('../views/AgentTest.vue'),
        meta: { title: 'Agent 测试' }
      },
      {
        path: 'rag',
        name: 'RagManage',
        component: () => import('../views/RagManage.vue'),
        meta: { title: 'RAG 管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - ZAgent Admin`
  }
  next()
})

export default router
