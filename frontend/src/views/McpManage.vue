<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">MCP 工具管理</h2>
      <div>
        <el-button @click="fetchRuntimeStatus" :icon="Refresh">刷新状态</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新建 MCP</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="mcps" stripe>
      <el-table-column prop="mcpId" label="MCP ID" width="120" />
      <el-table-column prop="mcpName" label="名称" width="180" />
      <el-table-column prop="transportType" label="传输类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.transportType === 'sse' ? 'warning' : ''" size="small">{{ row.transportType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="transportConfig" label="传输配置" show-overflow-tooltip />
      <el-table-column prop="requestTimeout" label="超时(分)" width="90" />
      <el-table-column label="运行状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="runtimeStatus[row.mcpId]" :type="runtimeStatus[row.mcpId].connected ? 'success' : 'danger'" size="small">
            {{ runtimeStatus[row.mcpId].connected ? '已连接' : '未连接' }}
          </el-tag>
          <el-tag v-else type="info" size="small">未知</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 MCP' : '新建 MCP'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="MCP ID" prop="mcpId">
          <el-input v-model="form.mcpId" :disabled="isEdit" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="名称" prop="mcpName">
          <el-input v-model="form.mcpName" placeholder="MCP 工具名称" />
        </el-form-item>
        <el-form-item label="传输类型" prop="transportType">
          <el-select v-model="form.transportType" style="width:100%">
            <el-option label="SSE (远程HTTP)" value="sse" />
            <el-option label="STDIO (本地进程)" value="stdio" />
          </el-select>
        </el-form-item>
        <el-form-item label="传输配置" prop="transportConfig">
          <el-input v-model="form.transportConfig" type="textarea" :rows="6" placeholder='JSON 格式' class="json-editor" />
        </el-form-item>
        <el-form-item label="超时(分钟)">
          <el-input-number v-model="form.requestTimeout" :min="1" :max="600" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mcpApi } from '../api'

const loading = ref(false)
const mcps = ref([])
const runtimeStatus = ref({})

const fetchMcps = async () => {
  loading.value = true
  try { mcps.value = await mcpApi.list() || [] } catch { /* */ } finally { loading.value = false }
}

const fetchRuntimeStatus = async () => {
  try { runtimeStatus.value = await mcpApi.runtimeStatus() || {} } catch { /* */ }
}

onMounted(() => { fetchMcps(); fetchRuntimeStatus() })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = ref({ mcpId: '', mcpName: '', transportType: 'sse', transportConfig: '', requestTimeout: 10, status: 1 })

const rules = {
  mcpId: [{ required: true, message: '请输入 MCP ID', trigger: 'blur' }],
  mcpName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  transportType: [{ required: true, message: '请选择传输类型', trigger: 'change' }],
  transportConfig: [{ required: true, message: '请输入传输配置', trigger: 'blur' }]
}

const openDialog = (row) => {
  isEdit.value = !!row
  form.value = row ? { ...row } : { mcpId: '', mcpName: '', transportType: 'sse', transportConfig: '', requestTimeout: 10, status: 1 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) { await mcpApi.update(form.value.mcpId, form.value); ElMessage.success('更新成功') }
    else { await mcpApi.create(form.value); ElMessage.success('创建成功') }
    dialogVisible.value = false; fetchMcps(); fetchRuntimeStatus()
  } catch { /* */ } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除 MCP "${row.mcpId}"？`, '删除确认', { type: 'warning' })
  try { await mcpApi.delete(row.mcpId); ElMessage.success('删除成功'); fetchMcps() } catch { /* */ }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
:deep(.json-editor textarea) { font-family: 'Courier New', monospace; font-size: 13px; line-height: 1.5; }
</style>
