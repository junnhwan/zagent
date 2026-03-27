<template>
  <div class="page-shell overview-shell">
    <section class="workspace-card info-card">
      <div>
        <p class="workspace-tag">Overview</p>
        <h1>工作台</h1>
        <p class="workspace-copy">从这里进入运行、查看最近结果和常用配置。</p>
      </div>
      <div class="workspace-actions">
        <el-button type="primary" @click="$router.push('/playground')">开始运行</el-button>
        <el-button @click="$router.push('/observability')">查看记录</el-button>
        <el-button @click="$router.push('/settings')">打开配置</el-button>
      </div>
    </section>

    <section class="overview-grid">
      <article class="info-card recent-card">
        <div class="section-heading">
          <h3>最近一次运行</h3>
          <p>保留最近一次输入和输出，方便继续查看。</p>
        </div>

        <template v-if="lastObservation">
          <div class="run-meta">
            <div><span>Agent</span><strong>{{ lastObservation.agentName || lastObservation.agentId || '-' }}</strong></div>
            <div><span>步骤数</span><strong>{{ lastObservation.steps?.length || 0 }}</strong></div>
            <div><span>记录时间</span><strong>{{ formatTime(lastObservation.createdAt) }}</strong></div>
          </div>
          <div class="preview-group">
            <div class="preview-item">
              <label>用户输入</label>
              <pre class="preview-block">{{ lastObservation.input || '暂无输入记录' }}</pre>
            </div>
            <div class="preview-item">
              <label>模型输出</label>
              <pre class="preview-block output">{{ lastObservation.finalOutput || '暂无输出记录' }}</pre>
            </div>
          </div>
          <div class="card-actions">
            <el-button type="primary" @click="$router.push('/playground')">继续运行</el-button>
            <el-button @click="$router.push('/observability')">查看完整记录</el-button>
          </div>
        </template>

        <article v-else class="empty-card">
          <h4>还没有运行记录</h4>
          <p>先到运行台发起一次任务，这里会自动显示最近一次输入和输出。</p>
          <el-button type="primary" @click="$router.push('/playground')">前往运行台</el-button>
        </article>
      </article>

      <div class="side-column">
        <article class="info-card">
          <div class="section-heading">
            <h3>系统状态</h3>
            <p>只展示最常看的状态信息。</p>
          </div>
          <div class="status-list">
            <div class="status-row">
              <span>Agent 数量</span>
              <strong>{{ agentCountLabel }}</strong>
            </div>
            <div class="status-row">
              <span>MCP 状态</span>
              <strong>{{ mcpStatusLabel }}</strong>
            </div>
            <div class="status-row">
              <span>知识标签</span>
              <strong>{{ ragCountLabel }}</strong>
            </div>
          </div>
        </article>

        <article class="info-card">
          <div class="section-heading">
            <h3>快捷入口</h3>
            <p>进入最常用的页面。</p>
          </div>
          <div class="quick-grid">
            <button
              v-for="item in quickActions"
              :key="item.path"
              type="button"
              class="quick-item"
              @click="$router.push(item.path)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </article>

        <article class="info-card">
          <div class="section-heading">
            <h3>常用配置</h3>
            <p>常改的配置可以从这里直达。</p>
          </div>
          <div class="link-list">
            <button
              v-for="item in configEntries"
              :key="item.path"
              type="button"
              class="link-item"
              @click="$router.push(item.path)"
            >
              <span>{{ item.title }}</span>
              <small>{{ item.desc }}</small>
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { agentApi, mcpApi, ragApi } from '../api'
import { normalizeMcpRuntimeStatus } from '../utils/mcpRuntime'
import { loadLastObservation } from '../utils/observability'

const quickActions = [
  { title: '开始运行', desc: '进入运行台发起任务', path: '/playground' },
  { title: '查看记录', desc: '打开最近一次运行记录', path: '/observability' },
  { title: '知识库', desc: '管理文档与标签', path: '/knowledge' }
]

const configEntries = [
  { title: 'Agent', desc: '运行入口与流程配置', path: '/settings/agents' },
  { title: 'Model', desc: '模型列表与能力', path: '/settings/models' },
  { title: 'Prompt', desc: '系统提示词模板', path: '/settings/prompts' },
  { title: 'MCP', desc: '工具服务与接入配置', path: '/settings/mcps' }
]

const lastObservation = ref(null)
const agentCount = ref(null)
const runtimeStatus = ref({})
const ragTagCount = ref(null)

const agentCountLabel = computed(() => {
  if (agentCount.value === null) {
    return '不可用'
  }
  return `${agentCount.value} 个`
})

const mcpStatusLabel = computed(() => {
  const values = Object.values(runtimeStatus.value || {})
  if (!values.length) {
    return '未获取'
  }
  const connected = values.filter((item) => item?.connected).length
  return `${connected}/${values.length} 已连接`
})

const ragCountLabel = computed(() => {
  if (ragTagCount.value === null) {
    return '不可用'
  }
  return `${ragTagCount.value} 个`
})

onMounted(async () => {
  lastObservation.value = loadLastObservation()

  try {
    const agents = (await agentApi.list()) || []
    agentCount.value = Array.isArray(agents) ? agents.length : 0
  } catch {
    agentCount.value = null
  }

  try {
    runtimeStatus.value = normalizeMcpRuntimeStatus(await mcpApi.runtimeStatus())
  } catch {
    runtimeStatus.value = {}
  }

  try {
    const tags = (await ragApi.tags()) || []
    ragTagCount.value = Array.isArray(tags) ? tags.length : 0
  } catch {
    ragTagCount.value = null
  }
})

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.workspace-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.workspace-tag {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #61708d;
}

.workspace-card h1 {
  margin: 0;
  font-size: 28px;
}

.workspace-copy {
  margin-top: 8px;
}

.workspace-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.run-meta,
.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.run-meta div,
.status-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.run-meta span,
.status-row span {
  color: #6c7791;
}

.preview-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.preview-item label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
}

.preview-block {
  min-height: 120px;
  max-height: 220px;
  overflow: auto;
  padding: 14px;
  border-radius: 12px;
  background: #f7f9fc;
  border: 1px solid #e7ebf2;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.65;
  color: #1f2a44;
}

.preview-block.output {
  min-height: 180px;
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.empty-card {
  margin-top: 16px;
  padding: 18px;
  border: 1px dashed #d8dfea;
  border-radius: 14px;
  background: #fafbfd;
}

.empty-card h4 {
  margin-bottom: 8px;
}

.quick-grid,
.link-list,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quick-item,
.link-item {
  width: 100%;
  text-align: left;
  border: 1px solid #e6ebf3;
  border-radius: 12px;
  background: #f8faff;
  cursor: pointer;
}

.quick-item {
  padding: 14px;
}

.quick-item strong,
.link-item span {
  display: block;
  color: #1f2a44;
}

.quick-item span,
.link-item small {
  display: block;
  margin-top: 6px;
  color: #6d7891;
  line-height: 1.55;
}

.link-item {
  padding: 14px 16px;
}

@media (max-width: 1100px) {
  .overview-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .workspace-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .workspace-card h1 {
    font-size: 24px;
  }
}
</style>
