import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// Response interceptor — unwrap { code: "0000", info, data }
http.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === '0000') {
      return res.data
    }
    ElMessage.error(res.info || '请求失败')
    return Promise.reject(new Error(res.info || '请求失败'))
  },
  (error) => {
    ElMessage.error(error.response?.data?.info || error.message || '网络异常')
    return Promise.reject(error)
  }
)

// ─── Agent ────────────────────────────────────────────
export const agentApi = {
  list: () => http.get('/admin/agents'),
  get: (agentId) => http.get(`/admin/agents/${agentId}`),
  create: (data) => http.post('/admin/agents', data),
  update: (agentId, data) => http.put(`/admin/agents/${agentId}`, data),
  delete: (agentId) => http.delete(`/admin/agents/${agentId}`)
}

// ─── FlowConfig ───────────────────────────────────────
export const flowApi = {
  list: (agentId) => http.get(`/admin/agents/${agentId}/flows`),
  create: (agentId, data) => http.post(`/admin/agents/${agentId}/flows`, data),
  update: (id, data) => http.put(`/admin/flows/${id}`, data),
  delete: (id) => http.delete(`/admin/flows/${id}`)
}

// ─── Client ───────────────────────────────────────────
export const clientApi = {
  list: () => http.get('/admin/clients'),
  create: (data) => http.post('/admin/clients', data),
  update: (clientId, data) => http.put(`/admin/clients/${clientId}`, data),
  delete: (clientId) => http.delete(`/admin/clients/${clientId}`)
}

// ─── API Config ───────────────────────────────────────
export const apiConfigApi = {
  list: () => http.get('/admin/apis'),
  create: (data) => http.post('/admin/apis', data),
  update: (apiId, data) => http.put(`/admin/apis/${apiId}`, data),
  delete: (apiId) => http.delete(`/admin/apis/${apiId}`)
}

// ─── Model ────────────────────────────────────────────
export const modelApi = {
  list: () => http.get('/admin/models'),
  create: (data) => http.post('/admin/models', data),
  update: (modelId, data) => http.put(`/admin/models/${modelId}`, data),
  delete: (modelId) => http.delete(`/admin/models/${modelId}`)
}

// ─── Prompt ───────────────────────────────────────────
export const promptApi = {
  list: () => http.get('/admin/prompts'),
  create: (data) => http.post('/admin/prompts', data),
  update: (promptId, data) => http.put(`/admin/prompts/${promptId}`, data),
  delete: (promptId) => http.delete(`/admin/prompts/${promptId}`)
}

// ─── Advisor ──────────────────────────────────────────
export const advisorApi = {
  list: () => http.get('/admin/advisors'),
  create: (data) => http.post('/admin/advisors', data),
  update: (advisorId, data) => http.put(`/admin/advisors/${advisorId}`, data),
  delete: (advisorId) => http.delete(`/admin/advisors/${advisorId}`)
}

// ─── MCP Tool ─────────────────────────────────────────
export const mcpApi = {
  list: () => http.get('/admin/mcps'),
  create: (data) => http.post('/admin/mcps', data),
  update: (mcpId, data) => http.put(`/admin/mcps/${mcpId}`, data),
  delete: (mcpId) => http.delete(`/admin/mcps/${mcpId}`),
  runtimeStatus: () => http.get('/admin/mcps/runtime-status')
}

// ─── Config Binding ───────────────────────────────────
export const configApi = {
  list: () => http.get('/admin/configs'),
  listByClient: (clientId) => http.get(`/admin/configs/client/${clientId}`),
  create: (data) => http.post('/admin/configs', data),
  delete: (id) => http.delete(`/admin/configs/${id}`)
}

// ─── Cache ────────────────────────────────────────────
export const cacheApi = {
  invalidateClient: (clientId) => http.post(`/admin/cache/invalidate/${clientId}`),
  invalidateAll: () => http.post('/admin/cache/invalidate-all')
}

// ─── Agent-as-Tool ────────────────────────────────────
export const agentToolApi = {
  list: () => http.get('/admin/agent-tools')
}

// ─── Chat ─────────────────────────────────────────────
export const chatApi = {
  send: (data) => http.post('/chat', data)
  // SSE stream uses native EventSource — see ChatTest.vue
}

// ─── Agent Run ────────────────────────────────────────
export const agentRunApi = {
  run: (data) => http.post('/agent/run', data, { timeout: 0 })
  // SSE stream uses native EventSource — see AgentTest.vue
}

// ─── RAG ──────────────────────────────────────────────
export const ragApi = {
  upload: (file, knowledgeTag) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('knowledgeTag', knowledgeTag)
    return http.post('/rag/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },
  query: (data) => http.post('/rag/query', data),
  deleteTag: (tag) => http.delete(`/rag/${tag}`),
  tags: () => http.get('/rag/tags')
}

export default http
