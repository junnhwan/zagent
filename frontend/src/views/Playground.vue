<template>
  <div class="page-shell">
    <section class="toolbar-card info-card">
      <div>
        <h1>运行台</h1>
        <p class="toolbar-copy">选择 Agent，输入任务，查看最终输出和步骤。</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="$router.push('/observability')">查看记录</el-button>
        <el-button @click="$router.push('/settings')">打开配置</el-button>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>运行参数</h3>
        <p>同步接口会在结果完整返回后展示输出和步骤。</p>
      </div>

      <div class="console-card">
        <el-row :gutter="16">
          <el-col :lg="10" :md="12" :sm="24">
            <el-form-item label="Agent">
              <el-select v-model="agentId" filterable placeholder="选择 Agent" style="width: 100%">
                <el-option
                  v-for="agent in agents"
                  :key="agent.agentId"
                  :label="`${agent.agentId} (${agent.agentName || ''}) [${agent.strategy}]`"
                  :value="agent.agentId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12" :sm="24">
            <el-form-item label="最大轮次">
              <el-slider v-model="maxStep" :min="1" :max="10" show-input size="small" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="24" :sm="24">
            <el-form-item label="当前策略">
              <el-input :model-value="selectedAgent?.strategy || '-'" readonly />
            </el-form-item>
          </el-col>
        </el-row>

        <el-input
          v-model="input"
          type="textarea"
          :rows="5"
          resize="none"
          placeholder="输入任务，例如：查询上海天气并给出今天的出行建议。"
        />

        <div class="console-actions">
          <el-button type="primary" :loading="running" :disabled="!canRun" @click="runAgent">
            开始运行
          </el-button>
          <el-button :disabled="!finalOutput" @click="copyFinalOutput">复制最终输出</el-button>
          <el-button :disabled="!steps.length" @click="copyStepsJson">复制步骤 JSON</el-button>
        </div>
      </div>
    </section>

    <section class="section-block" v-if="showResultSection">
      <el-alert
        v-if="running"
        class="result-alert"
        type="info"
        :closable="false"
        show-icon
        title="Agent 正在运行"
        description="同步模式会等待完整结果返回，请稍等片刻。"
      />
      <el-alert
        v-else-if="runState === 'error'"
        class="result-alert"
        type="error"
        :closable="false"
        show-icon
        title="本次运行未拿到可展示结果"
        :description="runError || '请检查网络请求和后端日志。'"
      />
      <el-alert
        v-else-if="runState === 'success' && !finalOutput && !steps.length"
        class="result-alert"
        type="warning"
        :closable="false"
        show-icon
        title="运行已完成，但返回内容为空"
        description="后端没有返回可展示内容，建议检查 Agent 流程配置。"
      />

      <div class="result-grid">
        <article class="info-card result-summary">
          <div class="section-heading">
            <h3>运行摘要</h3>
          </div>
          <div class="summary-list">
            <div>
              <span>运行状态</span>
              <strong>{{ runStatusLabel }}</strong>
            </div>
            <div>
              <span>Agent</span>
              <strong>{{ resultMeta.agentName || resultMeta.agentId || '-' }}</strong>
            </div>
            <div>
              <span>策略</span>
              <strong>{{ selectedAgent?.strategy || '-' }}</strong>
            </div>
            <div>
              <span>步骤数</span>
              <strong>{{ steps.length }}</strong>
            </div>
          </div>
        </article>

        <article class="info-card final-output-card">
          <div class="output-head">
            <h3>最终输出</h3>
          </div>
          <pre class="output-body">{{ finalOutput || '运行后将在这里展示最终输出。' }}</pre>
        </article>
      </div>
    </section>

    <section class="section-block" v-if="steps.length">
      <div class="section-heading">
        <h3>步骤列表</h3>
        <p>展示每一步的输入和输出。</p>
      </div>

      <el-collapse>
        <el-collapse-item v-for="step in steps" :key="step.sequence" :name="step.sequence">
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { agentApi, agentRunApi } from '../api'
import { saveLastObservation } from '../utils/observability'

const agents = ref([])
const agentId = ref('')
const input = ref('')
const maxStep = ref(3)
const running = ref(false)
const runState = ref('idle')
const runError = ref('')
const finalOutput = ref('')
const steps = ref([])
const resultMeta = ref({ agentId: '', agentName: '' })

const selectedAgent = computed(() => agents.value.find((agent) => agent.agentId === agentId.value) || null)
const canRun = computed(() => agentId.value && input.value.trim() && !running.value)
const hasResult = computed(() => Boolean(resultMeta.value.agentId || finalOutput.value || steps.value.length))
const showResultSection = computed(() => runState.value !== 'idle' || hasResult.value)
const runStatusLabel = computed(() => {
  if (running.value) {
    return '运行中'
  }
  if (runState.value === 'success') {
    return '已完成'
  }
  if (runState.value === 'error') {
    return '失败'
  }
  return '未开始'
})

onMounted(async () => {
  try {
    agents.value = (await agentApi.list()) || []
    if (!agentId.value && agents.value.length) {
      agentId.value = agents.value[0].agentId
    }
  } catch {
    // error handled by interceptor
  }
})

const runAgent = async () => {
  if (!canRun.value) {
    return
  }

  running.value = true
  runState.value = 'running'
  runError.value = ''
  finalOutput.value = ''
  steps.value = []
  resultMeta.value = { agentId: '', agentName: '' }

  try {
    const result = await agentRunApi.run({
      agentId: agentId.value,
      input: input.value.trim(),
      maxStep: maxStep.value,
      conversationId: null
    })

    resultMeta.value = {
      agentId: result?.agentId || agentId.value,
      agentName: result?.agentName || selectedAgent.value?.agentName || ''
    }
    finalOutput.value = result?.finalOutput || ''
    steps.value = Array.isArray(result?.steps) ? result.steps : []
    runState.value = 'success'

    try {
      saveLastObservation({
        lens: '',
        lensLabel: '',
        agentId: resultMeta.value.agentId,
        agentName: resultMeta.value.agentName,
        input: input.value.trim(),
        finalOutput: finalOutput.value,
        steps: steps.value
      })
    } catch (observationError) {
      ElMessage.warning(`运行结果已展示，但写入本地记录失败：${observationError?.message || '未知错误'}`)
    }

    ElMessage.success('运行完成')
  } catch (error) {
    runState.value = 'error'
    runError.value = error?.message || '运行失败'
  } finally {
    running.value = false
  }
}

const copyFinalOutput = async () => {
  await navigator.clipboard.writeText(finalOutput.value || '')
  ElMessage.success('已复制最终输出')
}

const copyStepsJson = async () => {
  await navigator.clipboard.writeText(JSON.stringify(steps.value, null, 2))
  ElMessage.success('已复制步骤 JSON')
}
</script>

<style scoped>
.toolbar-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-card h1 {
  margin: 0;
  font-size: 28px;
}

.toolbar-copy {
  margin-top: 8px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.console-card {
  padding: 24px;
  border: 1px solid #e6ebf3;
  border-radius: 18px;
  background: #ffffff;
}

.console-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.result-alert {
  margin-bottom: 16px;
}

.result-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
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

.output-body {
  min-height: 160px;
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

@media (max-width: 768px) {
  .toolbar-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-card h1 {
    font-size: 24px;
  }
}
</style>
