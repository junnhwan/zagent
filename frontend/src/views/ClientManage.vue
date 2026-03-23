<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">Client 管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建 Client</el-button>
    </div>

    <!-- Client table -->
    <el-table v-loading="loading" :data="clients" stripe highlight-current-row @row-click="handleRowClick" row-class-name="clickable-row">
      <el-table-column prop="clientId" label="Client ID" width="200" />
      <el-table-column prop="clientName" label="名称" width="200" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDialog(row)">编辑</el-button>
          <el-button link type="warning" @click.stop="handleInvalidateCache(row)">刷新缓存</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Config Bindings panel -->
    <el-card v-if="selectedClient" class="binding-card">
      <template #header>
        <div class="binding-header">
          <span>配置绑定 - {{ selectedClient.clientName }} ({{ selectedClient.clientId }})</span>
          <el-button type="primary" size="small" :icon="Plus" @click="openBindingDialog()">添加绑定</el-button>
        </div>
      </template>
      <el-table v-loading="bindingLoading" :data="bindings" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="sourceType" label="源类型" width="120" />
        <el-table-column prop="sourceId" label="源 ID" width="180" />
        <el-table-column prop="targetType" label="目标类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="targetTypeTag(row.targetType)">{{ row.targetType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标 ID" width="180" />
        <el-table-column prop="extParam" label="扩展参数" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDeleteBinding(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Client dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 Client' : '新建 Client'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="Client ID" prop="clientId">
          <el-input v-model="form.clientId" :disabled="isEdit" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="名称" prop="clientName">
          <el-input v-model="form.clientName" placeholder="Client 名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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

    <!-- Binding dialog -->
    <el-dialog v-model="bindingDialogVisible" title="添加配置绑定" width="560px" destroy-on-close>
      <el-form ref="bindingFormRef" :model="bindingForm" :rules="bindingRules" label-width="100px">
        <el-form-item label="目标类型" prop="targetType">
          <el-select v-model="bindingForm.targetType" placeholder="选择目标类型" style="width:100%" @change="handleTargetTypeChange">
            <el-option label="模型 (model)" value="model" />
            <el-option label="提示词 (prompt)" value="prompt" />
            <el-option label="Advisor" value="advisor" />
            <el-option label="MCP 工具 (tool_mcp)" value="tool_mcp" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标 ID" prop="targetId">
          <el-select v-model="bindingForm.targetId" filterable placeholder="选择目标" style="width:100%">
            <el-option v-for="t in targetOptions" :key="t.id" :label="t.label" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="扩展参数">
          <el-input v-model="bindingForm.extParam" type="textarea" :rows="3" placeholder="JSON 格式的扩展参数 (可选)" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="bindingForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindingSubmitting" @click="handleBindingSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clientApi, configApi, cacheApi, modelApi, promptApi, advisorApi, mcpApi } from '../api'

// ─── Client list ──────────────────────────────────────
const loading = ref(false)
const clients = ref([])

const fetchClients = async () => {
  loading.value = true
  try { clients.value = await clientApi.list() || [] } catch { /* */ } finally { loading.value = false }
}

onMounted(fetchClients)

// ─── Client CRUD ──────────────────────────────────────
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = ref({ clientId: '', clientName: '', description: '', status: 1 })

const rules = {
  clientId: [{ required: true, message: '请输入 Client ID', trigger: 'blur' }],
  clientName: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const openDialog = (row) => {
  isEdit.value = !!row
  form.value = row ? { ...row } : { clientId: '', clientName: '', description: '', status: 1 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await clientApi.update(form.value.clientId, form.value)
      ElMessage.success('更新成功')
    } else {
      await clientApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchClients()
  } catch { /* */ } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除 Client "${row.clientId}"？`, '删除确认', { type: 'warning' })
  try {
    await clientApi.delete(row.clientId)
    ElMessage.success('删除成功')
    if (selectedClient.value?.clientId === row.clientId) {
      selectedClient.value = null
      bindings.value = []
    }
    fetchClients()
  } catch { /* */ }
}

const handleInvalidateCache = async (row) => {
  try {
    await cacheApi.invalidateClient(row.clientId)
    ElMessage.success(`Client "${row.clientId}" 缓存已刷新`)
  } catch { /* */ }
}

// ─── Config Bindings ──────────────────────────────────
const selectedClient = ref(null)
const bindingLoading = ref(false)
const bindings = ref([])

const handleRowClick = async (row) => {
  selectedClient.value = row
  bindingLoading.value = true
  try { bindings.value = await configApi.listByClient(row.clientId) || [] } catch { bindings.value = [] } finally { bindingLoading.value = false }
}

const targetTypeTag = (type) => {
  const map = { model: '', prompt: 'success', advisor: 'warning', mcp: 'danger' }
  return map[type] || 'info'
}

// ─── Add binding ──────────────────────────────────────
const bindingDialogVisible = ref(false)
const bindingSubmitting = ref(false)
const bindingFormRef = ref()
const bindingForm = ref({ targetType: '', targetId: '', extParam: '', status: 1 })
const targetOptions = ref([])

const bindingRules = {
  targetType: [{ required: true, message: '请选择目标类型', trigger: 'change' }],
  targetId: [{ required: true, message: '请选择目标', trigger: 'change' }]
}

const openBindingDialog = () => {
  bindingForm.value = { targetType: '', targetId: '', extParam: '', status: 1 }
  targetOptions.value = []
  bindingDialogVisible.value = true
}

const handleTargetTypeChange = async (type) => {
  bindingForm.value.targetId = ''
  targetOptions.value = []
  try {
    let list = []
    if (type === 'model') { list = await modelApi.list(); targetOptions.value = (list || []).map(i => ({ id: i.modelId, label: `${i.modelId} (${i.modelName || ''})` })) }
    else if (type === 'prompt') { list = await promptApi.list(); targetOptions.value = (list || []).map(i => ({ id: i.promptId, label: `${i.promptId} (${i.promptName || ''})` })) }
    else if (type === 'advisor') { list = await advisorApi.list(); targetOptions.value = (list || []).map(i => ({ id: i.advisorId, label: `${i.advisorId} (${i.advisorName || ''})` })) }
    else if (type === 'tool_mcp') { list = await mcpApi.list(); targetOptions.value = (list || []).map(i => ({ id: i.mcpId, label: `${i.mcpId} (${i.mcpName || ''})` })) }
  } catch { /* */ }
}

const handleBindingSubmit = async () => {
  await bindingFormRef.value.validate()
  bindingSubmitting.value = true
  try {
    await configApi.create({
      sourceType: 'client',
      sourceId: selectedClient.value.clientId,
      targetType: bindingForm.value.targetType,
      targetId: bindingForm.value.targetId,
      extParam: bindingForm.value.extParam || null,
      status: bindingForm.value.status
    })
    ElMessage.success('绑定添加成功')
    bindingDialogVisible.value = false
    handleRowClick(selectedClient.value)
  } catch { /* */ } finally { bindingSubmitting.value = false }
}

const handleDeleteBinding = async (row) => {
  await ElMessageBox.confirm(`确认删除此绑定？`, '删除确认', { type: 'warning' })
  try {
    await configApi.delete(row.id)
    ElMessage.success('删除成功')
    handleRowClick(selectedClient.value)
  } catch { /* */ }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
.binding-card { margin-top: 20px; }
.binding-header { display: flex; justify-content: space-between; align-items: center; }
:deep(.clickable-row) { cursor: pointer; }
</style>
