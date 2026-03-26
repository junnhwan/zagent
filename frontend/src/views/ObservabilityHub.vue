<template>
  <div class="page-shell">
    <section class="hero-panel compact">
      <div>
        <p class="eyebrow">Observability</p>
        <h1>把最近一次 Agent 运行变成可讲解、可导出的证据</h1>
        <p class="hero-copy">
          这里优先服务演示与面试：聚合最近一次 Playground 同步运行、MCP 运行状态与 RAG tags 概览，帮助你解释 Agent 是如何工作的。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="$router.push('/playground')">回到 Playground</el-button>
        <el-button :disabled="!observation" @click="copyObservationJson">复制观测 JSON</el-button>
        <el-button :disabled="!observation" @click="downloadObservation">下载观测文件</el-button>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>最近一次运行摘要</h3>
        <p>先在 Playground 发起一次同步运行，再回到这里查看证据。</p>
      </div>

      <div v-if="observation" class="result-grid">
        <article class="info-card">
          <div class="summary-list">
            <div><span>范式视角</span><strong>{{ observation.lensLabel || observation.lens || '-' }}</strong></div>
            <div><span>Agent</span><strong>{{ observation.agentName || observation.agentId || '-' }}</strong></div>
            <div><span>步骤数</span><strong>{{ observation.steps?.length || 0 }}</strong></div>
            <div><span>记录时间</span><strong>{{ formatTime(observation.createdAt) }}</strong></div>
          </div>
        </article>

        <article class="info-card final-output-card">
          <div class="output-head">
            <h3>最终输出摘要</h3>
            <el-tag type="success">最近一次运行</el-tag>
          </div>
          <pre class="output-body">{{ observation.finalOutput || '暂无最终输出' }}</pre>
        </article>
      </div>

      <article v-else class="info-card empty-state">
        <h3>未发现运行记录</h3>
        <p>请先去 `Playground` 运行一次同步任务，这里会自动展示最近一次运行证据。</p>
      </article>
    </section>

    <section class="section-block" v-if="observation?.steps?.length">
      <div class="section-heading">
        <h3>Steps 明细</h3>
        <p>这里复用 `AgentResultVO.steps`，用于说明 Agent 的处理过程。</p>
      </div>
      <el-collapse>
        <el-collapse-item v-for="step in observation.steps" :key="step.sequence" :name="step.sequence">
          <template #title>
            <div class="step-title">
              <span>Step {{ step.sequence }}</span>
              <el-tag size="small" effect="plain">{{ step.clientId || 'unknown-client' }}</el-tag>
            </div>
          </template>
          <div class="step-body">
            <div class="step-field">
              <label>输入</label>
              <pre>{{ step.input || '-' }}</pre>
            </div>
            <div class="step-field">
              <label>输出</label>
              <pre>{{ step.output || '-' }}</pre>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </section>

    <section class="section-block">
      <div class="card-grid cols-2">
        <article class="info-card">
          <div class="output-head">
            <h3>MCP Runtime Status</h3>
            <el-tag>{{ runtimeEntries.length }} 项</el-tag>
          </div>
          <p v-if="runtimeLoadError" class="muted">不可用：请求失败</p>
          <div v-else-if="runtimeEntries.length" class="status-list">
            <div v-for="item in runtimeEntries" :key="item.key" class="status-row">
              <span>{{ item.key }}</span>
              <el-tag :type="item.value.connected ? 'success' : 'danger'" size="small">
                {{ item.value.connected ? '已连接' : '未连接' }}
              </el-tag>
            </div>
          </div>
          <p v-else class="muted">当前未获取到 MCP 运行状态。</p>
        </article>

        <article class="info-card">
          <div class="output-head">
            <h3>RAG Tags 概览</h3>
            <el-tag>{{ ragTags.length }} 个</el-tag>
          </div>
          <p v-if="ragLoadError" class="muted">不可用：请求失败</p>
          <div v-else-if="ragTags.length" class="tag-wrap">
            <el-tag v-for="tag in ragTags" :key="tag.tag || tag" effect="plain">
              {{ tag.tag || tag }}
            </el-tag>
          </div>
          <p v-else class="muted">当前未获取到知识标签。</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { mcpApi, ragApi } from '../api'
import { exportObservationToFile, loadLastObservation } from '../utils/observability'

const observation = ref(null)
const runtimeStatus = ref({})
const ragTags = ref([])
const runtimeLoadError = ref(false)
const ragLoadError = ref(false)

const runtimeEntries = computed(() => Object.entries(runtimeStatus.value).map(([key, value]) => ({ key, value })))

onMounted(async () => {
  observation.value = loadLastObservation()
  try {
    runtimeStatus.value = (await mcpApi.runtimeStatus()) || {}
    runtimeLoadError.value = false
  } catch {
    runtimeStatus.value = {}
    runtimeLoadError.value = true
  }
  try {
    ragTags.value = (await ragApi.tags()) || []
    ragLoadError.value = false
  } catch {
    ragTags.value = []
    ragLoadError.value = true
  }
})

const copyObservationJson = async () => {
  await navigator.clipboard.writeText(JSON.stringify(observation.value, null, 2))
  ElMessage.success('已复制观测 JSON')
}

const downloadObservation = () => {
  exportObservationToFile(observation.value)
  ElMessage.success('已下载观测文件')
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.empty-state {
  text-align: center;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.muted {
  color: #6d7891;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary-list div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.summary-list span {
  color: #6c7791;
}

.output-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.output-body,
.step-field pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  line-height: 1.75;
  color: #1f2a44;
}

.step-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.step-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.step-field label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
