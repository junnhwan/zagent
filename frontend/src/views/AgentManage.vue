<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">Agent 管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建 Agent</el-button>
    </div>

    <!-- Agent table -->
    <el-table
      v-loading="loading"
      :data="agents"
      stripe
      highlight-current-row
      @row-click="handleRowClick"
      row-class-name="clickable-row"
    >
      <el-table-column prop="agentId" label="Agent ID" width="180" />
      <el-table-column prop="agentName" label="名称" width="180" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="strategy" label="策略" width="100">
        <template #default="{ row }">
          <el-tag :type="strategyTag(row.strategy)" size="small">{{ row.strategy }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="channel" label="渠道" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- FlowConfig panel -->
    <el-card v-if="selectedAgent" class="flow-card">
      <template #header>
        <div class="flow-header">
          <span>流程配置 - {{ selectedAgent.agentName }} ({{ selectedAgent.agentId }})</span>
          <el-button type="primary" size="small" :icon="Plus" @click="openFlowDialog()">添加步骤</el-button>
        </div>
      </template>
      <el-table v-loading="flowLoading" :data="flows" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="clientId" label="Client ID" width="180" />
        <el-table-column prop="clientType" label="客户端类型" width="120" />
        <el-table-column prop="sequence" label="顺序" width="80" />
        <el-table-column prop="stepPrompt" label="步骤提示词" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openFlowDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteFlow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Agent dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 Agent' : '新建 Agent'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="Agent ID" prop="agentId">
          <el-input v-model="form.agentId" :disabled="isEdit" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="名称" prop="agentName">
          <el-input v-model="form.agentName" placeholder="Agent 名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述" />
        </el-form-item>
        <el-form-item label="策略" prop="strategy">
          <el-select v-model="form.strategy" placeholder="选择策略" style="width:100%">
            <el-option label="fixed" value="fixed" />
            <el-option label="auto" value="auto" />
            <el-option label="flow" value="flow" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-input v-model="form.channel" placeholder="渠道标识" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Flow dialog -->
    <el-dialog v-model="flowDialogVisible" :title="isFlowEdit ? '编辑步骤' : '添加步骤'" width="560px" destroy-on-close>
      <el-form ref="flowFormRef" :model="flowForm" :rules="flowRules" label-width="100px">
        <el-form-item label="Client ID" prop="clientId">
          <el-select v-model="flowForm.clientId" filterable placeholder="选择 Client" style="width:100%">
            <el-option v-for="c in clientList" :key="c.clientId" :label="`${c.clientId} (${c.clientName || ''})`" :value="c.clientId" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户端类型" prop="clientType">
          <el-input v-model="flowForm.clientType" placeholder="如: planner, executor, supervisor" />
        </el-form-item>
        <el-form-item label="顺序" prop="sequence">
          <el-input-number v-model="flowForm.sequence" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="步骤提示词">
          <el-input v-model="flowForm.stepPrompt" type="textarea" :rows="4" placeholder="可选的步骤提示词" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="flowDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="flowSubmitting" @click="handleFlowSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agentApi, flowApi, clientApi } from '../api'

// ─── Agent list ───────────────────────────────────────
const loading = ref(false)
const agents = ref([])
const clientList = ref([])

const fetchAgents = async () => {
  loading.value = true
  try {
    agents.value = await agentApi.list() || []
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

const fetchClients = async () => {
  try { clientList.value = await clientApi.list() || [] } catch { /* */ }
}

onMounted(() => { fetchAgents(); fetchClients() })

const strategyTag = (s) => {
  const map = { fixed: '', auto: 'warning', flow: 'success' }
  return map[s] || 'info'
}

// ─── Agent CRUD ───────────────────────────────────────
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = ref({ agentId: '', agentName: '', description: '', strategy: 'fixed', channel: '', status: 1 })

const rules = {
  agentId: [{ required: true, message: '请输入 Agent ID', trigger: 'blur' }],
  agentName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  strategy: [{ required: true, message: '请选择策略', trigger: 'change' }]
}

const openDialog = (row) => {
  isEdit.value = !!row
  form.value = row
    ? { ...row }
    : { agentId: '', agentName: '', description: '', strategy: 'fixed', channel: '', status: 1 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await agentApi.update(form.value.agentId, form.value)
      ElMessage.success('更新成功')
    } else {
      await agentApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchAgents()
  } catch { /* */ } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除 Agent "${row.agentId}" 及其所有流程配置？`, '删除确认', { type: 'warning' })
  try {
    await agentApi.delete(row.agentId)
    ElMessage.success('删除成功')
    if (selectedAgent.value?.agentId === row.agentId) {
      selectedAgent.value = null
      flows.value = []
    }
    fetchAgents()
  } catch { /* */ }
}

// ─── FlowConfig ───────────────────────────────────────
const selectedAgent = ref(null)
const flowLoading = ref(false)
const flows = ref([])

const handleRowClick = async (row) => {
  selectedAgent.value = row
  flowLoading.value = true
  try {
    flows.value = await flowApi.list(row.agentId) || []
  } catch { flows.value = [] } finally {
    flowLoading.value = false
  }
}

const flowDialogVisible = ref(false)
const isFlowEdit = ref(false)
const flowSubmitting = ref(false)
const flowFormRef = ref()
const flowForm = ref({ clientId: '', clientType: '', sequence: 0, stepPrompt: '' })

const flowRules = {
  clientId: [{ required: true, message: '请选择 Client', trigger: 'change' }],
  clientType: [{ required: true, message: '请输入客户端类型', trigger: 'blur' }]
}

const openFlowDialog = (row) => {
  isFlowEdit.value = !!row
  flowForm.value = row
    ? { ...row }
    : { clientId: '', clientType: '', sequence: (flows.value.length + 1) * 10, stepPrompt: '' }
  flowDialogVisible.value = true
}

const handleFlowSubmit = async () => {
  await flowFormRef.value.validate()
  flowSubmitting.value = true
  try {
    if (isFlowEdit.value) {
      await flowApi.update(flowForm.value.id, flowForm.value)
      ElMessage.success('更新成功')
    } else {
      await flowApi.create(selectedAgent.value.agentId, flowForm.value)
      ElMessage.success('添加成功')
    }
    flowDialogVisible.value = false
    handleRowClick(selectedAgent.value)
  } catch { /* */ } finally {
    flowSubmitting.value = false
  }
}

const handleDeleteFlow = async (row) => {
  await ElMessageBox.confirm(`确认删除步骤 #${row.id}？`, '删除确认', { type: 'warning' })
  try {
    await flowApi.delete(row.id)
    ElMessage.success('删除成功')
    handleRowClick(selectedAgent.value)
  } catch { /* */ }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
.flow-card { margin-top: 20px; }
.flow-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
:deep(.clickable-row) { cursor: pointer; }
</style>
