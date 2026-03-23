<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">Agent 测试</h2>
    </div>

    <el-row :gutter="20" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-form-item label="Agent">
          <el-select v-model="agentId" filterable placeholder="选择 Agent" style="width:100%">
            <el-option v-for="a in agents" :key="a.agentId" :label="`${a.agentId} (${a.agentName || ''}) [${a.strategy}]`" :value="a.agentId" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="4">
        <el-form-item label="最大轮次">
          <el-slider v-model="maxStep" :min="1" :max="10" show-input size="small" />
        </el-form-item>
      </el-col>
    </el-row>

    <div class="input-row">
      <el-input v-model="input" placeholder="输入任务..." :disabled="running" @keyup.enter="runAgent" size="large">
        <template #append>
          <el-button type="primary" @click="runAgent" :disabled="!agentId || !input.trim() || running" :loading="running">执行</el-button>
        </template>
      </el-input>
    </div>

    <!-- Stage Timeline -->
    <div v-if="stages.length" class="timeline-container">
      <el-timeline>
        <el-timeline-item v-for="(stage, idx) in stages" :key="idx"
          :type="stage.status === 'done' ? 'success' : stage.status === 'error' ? 'danger' : 'primary'"
          :hollow="stage.status === 'active'"
          :timestamp="`第${stage.step}轮 · ${stageLabel(stage.stage)}`" placement="top">
          <el-card shadow="never" class="stage-card">
            <div class="stage-content" v-if="stage.content">{{ truncate(stage.content, 500) }}</div>
            <div v-else class="stage-loading">处理中...</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>

    <!-- Final Result -->
    <el-card v-if="finalResult" class="result-card">
      <template #header><span style="font-weight:600">最终结果</span></template>
      <div class="result-content" v-html="finalResult"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { agentApi } from '../api'

const agents = ref([])
const agentId = ref('')
const input = ref('')
const maxStep = ref(3)
const running = ref(false)
const stages = ref([])
const finalResult = ref('')

onMounted(async () => { try { agents.value = await agentApi.list() || [] } catch { /* */ } })

const stageLabel = (stage) => {
  const map = { analysis: '分析', execution: '执行', supervision: '监督', summary: '总结', planning: '规划', tool_analysis: '工具分析', step_execution: '步骤执行', replanning: '重规划', reasoning: '推理', action: '工具调用', observation: '观察', final: '最终答案', complete: '完成' }
  return map[stage] || stage
}

const truncate = (text, max) => text && text.length > max ? text.substring(0, max) + '...' : text

const runAgent = () => {
  if (!agentId.value || !input.value.trim() || running.value) return
  running.value = true
  stages.value = []
  finalResult.value = ''

  const params = new URLSearchParams({ agentId: agentId.value, input: input.value.trim(), maxStep: maxStep.value })
  const evtSource = new EventSource(`/api/agent/run/stream?${params}`)

  evtSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'token') return // token 事件跳过时间线
      if (data.stage === 'complete') {
        evtSource.close()
        running.value = false
        return
      }
      if (data.stage === 'summary' && data.status === 'done' && data.content) {
        finalResult.value = data.content
      }
      if (data.status === 'done' || data.status === 'error') {
        stages.value.push(data)
      }
    } catch { /* */ }
  }

  evtSource.onerror = () => {
    evtSource.close()
    running.value = false
  }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { margin-bottom: 12px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
.input-row { margin-bottom: 20px; }
.timeline-container { margin-top: 16px; max-height: 500px; overflow-y: auto; }
.stage-card { margin-bottom: 0; }
.stage-content { white-space: pre-wrap; font-size: 13px; line-height: 1.6; max-height: 200px; overflow-y: auto; }
.stage-loading { color: #409eff; font-size: 13px; }
.result-card { margin-top: 20px; }
.result-content { white-space: pre-wrap; line-height: 1.7; }
</style>
