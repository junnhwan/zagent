<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">API 管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建 API</el-button>
    </div>

    <el-table v-loading="loading" :data="apis" stripe>
      <el-table-column prop="apiId" label="API ID" width="180" />
      <el-table-column prop="baseUrl" label="Base URL" show-overflow-tooltip />
      <el-table-column label="API Key" width="160">
        <template #default="{ row }">
          <span class="masked-key">{{ maskKey(row.apiKey) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="completionsPath" label="Completions 路径" width="180" show-overflow-tooltip />
      <el-table-column prop="embeddingsPath" label="Embeddings 路径" width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 API' : '新建 API'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="API ID" prop="apiId">
          <el-input v-model="form.apiId" :disabled="isEdit" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.openai.com" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="Completions 路径">
          <el-input v-model="form.completionsPath" placeholder="/v1/chat/completions" />
        </el-form-item>
        <el-form-item label="Embeddings 路径">
          <el-input v-model="form.embeddingsPath" placeholder="/v1/embeddings" />
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
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiConfigApi } from '../api'

const loading = ref(false)
const apis = ref([])

const fetchApis = async () => {
  loading.value = true
  try { apis.value = await apiConfigApi.list() || [] } catch { /* */ } finally { loading.value = false }
}

onMounted(fetchApis)

const maskKey = (key) => {
  if (!key) return '-'
  if (key.length <= 4) return '****'
  return '****' + key.slice(-4)
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = ref({ apiId: '', baseUrl: '', apiKey: '', completionsPath: '', embeddingsPath: '', status: 1 })

const rules = {
  apiId: [{ required: true, message: '请输入 API ID', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }]
}

const openDialog = (row) => {
  isEdit.value = !!row
  form.value = row
    ? { ...row }
    : { apiId: '', baseUrl: '', apiKey: '', completionsPath: '', embeddingsPath: '', status: 1 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await apiConfigApi.update(form.value.apiId, form.value)
      ElMessage.success('更新成功')
    } else {
      await apiConfigApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchApis()
  } catch { /* */ } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除 API "${row.apiId}"？`, '删除确认', { type: 'warning' })
  try {
    await apiConfigApi.delete(row.apiId)
    ElMessage.success('删除成功')
    fetchApis()
  } catch { /* */ }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
.masked-key { font-family: monospace; color: #909399; }
</style>
