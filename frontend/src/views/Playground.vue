<template>
  <div class="page-shell">
    <section class="hero-panel compact">
      <div>
        <p class="eyebrow">Playground</p>
        <h1>在一次演示运行里讲清 Agent 范式、执行步骤与最终输出</h1>
        <p class="hero-copy">
          这里是 Sprint 2 的主演示区：选择编排范式视角、选择 Agent、输入任务，然后查看步骤化结果与最终输出。
        </p>
      </div>
      <div class="hero-actions">
        <el-button @click="$router.push('/chat')">去聊天测试</el-button>
        <el-button @click="$router.push('/agent-test')">查看旧版运行页</el-button>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>编排范式视角</h3>
        <p>该视角用于讲解 Agent 的工作方式，不强制要求后端策略命名完全一致。</p>
      </div>
      <div class="mode-grid">
        <article
          v-for="mode in modes"
          :key="mode.value"
          class="mode-card"
          :class="{ active: selectedMode === mode.value }"
          @click="selectedMode = mode.value"
        >
          <div class="mode-head">
            <h4>{{ mode.label }}</h4>
            <span>{{ mode.tag }}</span>
          </div>
          <p>{{ mode.desc }}</p>
          <small>{{ mode.mapping }}</small>
        </article>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <h3>运行控制台</h3>
        <p>优先使用同步接口完成一次完整运行，便于稳定展示步骤与结果。</p>
      </div>

      <div class="console-card">
        <el-row :gutter="16">
          <el-col :lg="8" :md="12" :sm="24">
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
          <el-col :lg="10" :md="24" :sm="24">
            <el-form-item label="演示提示">
              <el-input :model-value="activeMode.tip" readonly />
            </el-form-item>
          </el-col>
        </el-row>

        <el-input
          v-model="input"
          type="textarea"
          :rows="4"
          resize="none"
          placeholder="输入一段适合演示 Agent 规划、工具调用或反思的任务，例如：帮我规划一次杭州两日技术旅行，要求兼顾美食和 AI 主题打卡。"
        />

        <div class="console-actions">
          <el-button type="primary" :loading="running" :disabled="!canRun" @click="runAgent">
            运行演示
          </el-button>
          <el-button :disabled="!finalOutput" @click="copyFinalOutput">复制最终输出</el-button>
          <el-button :disabled="!steps.length" @click="copyStepsJson">复制步骤 JSON</el-button>
        </div>
      </div>
    </section>

    <section class="section-block" v-if="resultMeta.agentId || finalOutput || steps.length">
      <div class="result-grid">
        <article class="info-card result-summary">
          <div class="section-heading">
            <h3>运行摘要</h3>
          </div>
          <div class="summary-list">
            <div>
              <span>当前视角</span>
              <strong>{{ activeMode.label }}</strong>
            </div>
            <div>
              <span>Agent</span>
              <strong>{{ resultMeta.agentName || resultMeta.agentId || '-' }}</strong>
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
            <el-tag type="primary">{{ activeMode.label }}</el-tag>
          </div>
          <pre class="output-body">{{ finalOutput || '运行后将在这里展示最终输出。' }}</pre>
        </article>
      </div>
    </section>

    <section class="section-block" v-if="steps.length">
      <div class="section-heading">
        <h3>步骤列表</h3>
        <p>至少展示 `sequence / clientId / input / output`，用于解释 Agent 如何得到结果。</p>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { agentApi, agentRunApi } from '../api'

const router = useRouter()
const route = useRoute()

const modes = [
  {
    value: 'plan-execute',
    label: 'Plan and Execute',
    tag: '规划优先',
    desc: '先拆任务，再按阶段执行，适合展示任务分解与执行链路。',
    mapping: '前端视角：映射为“先规划后执行”的讲解框架。',
    tip: '适合演示任务拆解、阶段产出和多步规划。'
  },
  {
    value: 'react',
    label: 'ReAct',
    tag: '交互闭环',
    desc: '边思考边行动，突出工具调用、观察反馈与动态调整。',
    mapping: '前端视角：映射为“思考-行动-观察”的闭环。',
    tip: '适合演示工具调用、反馈回流和过程推理。'
  },
  {
    value: 'reflection',
    label: 'Reflection',
    tag: '复盘优化',
    desc: '执行后复盘并修正，强调结果质量与稳定性提升。',
    mapping: '前端视角：映射为“先产出、再反思、再修正”。',
    tip: '适合演示结果审查、迭代优化和质量提升。'
  }
]

const agents = ref([])
const selectedMode = ref('react')
const agentId = ref('')
const input = ref('')
const maxStep = ref(3)
const running = ref(false)
const finalOutput = ref('')
const steps = ref([])
const resultMeta = ref({ agentId: '', agentName: '' })

const activeMode = computed(() => modes.find((mode) => mode.value === selectedMode.value) || modes[1])
const canRun = computed(() => agentId.value && input.value.trim() && !running.value)

const syncModeWithRoute = () => {
  const mode = route.query.mode
  if (typeof mode === 'string' && modes.some((item) => item.value === mode)) {
    selectedMode.value = mode
  }
}

watch(
  () => route.query.mode,
  () => syncModeWithRoute(),
  { immediate: true }
)

watch(selectedMode, (mode) => {
  if (route.query.mode !== mode) {
    router.replace({ query: { ...route.query, mode } })
  }
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
      agentName: result?.agentName || ''
    }
    finalOutput.value = result?.finalOutput || ''
    steps.value = Array.isArray(result?.steps) ? result.steps : []
    ElMessage.success('演示运行完成')
  } catch {
    finalOutput.value = ''
    steps.value = []
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
.mode-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.mode-card {
  padding: 20px;
  border: 1px solid #e8ecf5;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.mode-card:hover,
.mode-card.active {
  transform: translateY(-2px);
  border-color: #7d90ff;
  box-shadow: 0 16px 36px rgba(64, 99, 255, 0.10);
}

.mode-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.mode-head h4 {
  font-size: 18px;
}

.mode-head span {
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4257d6;
  font-size: 12px;
}

.mode-card p,
.mode-card small {
  color: #5f6b85;
  line-height: 1.7;
}

.mode-card small {
  display: block;
  margin-top: 10px;
}

.console-card {
  padding: 24px;
  border: 1px solid #e8ecf5;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
}

.console-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
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
  .mode-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
