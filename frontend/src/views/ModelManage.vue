<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">模型管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建模型</el-button>
    </div>

    <el-table v-loading="loading" :data="models" stripe>
      <el-table-column prop="modelId" label="模型 ID" width="200" />
      <el-table-column prop="apiId" label="API ID" width="180">
        <template #default="{ row }">
          <el-tag size="small">{{ row.apiId }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="modelName" label="模型名称" width="220" />
      <el-table-column prop="modelType" label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="row.modelType === 'CHAT' ? '' : 'warning'" size="small">{{ row.modelType }}</el-tag>
        </template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模型' : '新建模型'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模型 ID" prop="modelId">
          <el-input v-model="form.modelId" :disabled="isEdit" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="API" prop="apiId">
          <el-select v-model="form.apiId" filterable placeholder="选择 API 配置" style="width:100%">
            <el-option v-for="a in apiList" :key="a.apiId" :label="`${a.apiId} (${a.baseUrl || ''})`" :value="a.apiId" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如 gpt-4o, deepseek-chat" />
        </el-form-item>
        <el-form-item label="类型" prop="modelType">
          <el-select v-model="form.modelType" placeholder="选择类型" style="width:100%">
            <el-option label="CHAT" value="CHAT" />
            <el-option label="EMBEDDING" value="EMBEDDING" />
          </el-select>
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
import { modelApi, apiConfigApi } from '../api'

const loading = ref(false)
const models = ref([])
const apiList = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    const [m, a] = await Promise.all([modelApi.list(), apiConfigApi.list()])
    models.value = m || []
    apiList.value = a || []
  } catch { /* */ } finally { loading.value = false }
}

onMounted(fetchData)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = ref({ modelId: '', apiId: '', modelName: '', modelType: 'CHAT', status: 1 })

const rules = {
  modelId: [{ required: true, message: '请输入模型 ID', trigger: 'blur' }],
  apiId: [{ required: true, message: '请选择 API', trigger: 'change' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const openDialog = (row) => {
  isEdit.value = !!row
  form.value = row ? { ...row } : { modelId: '', apiId: '', modelName: '', modelType: 'CHAT', status: 1 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await modelApi.update(form.value.modelId, form.value)
      ElMessage.success('更新成功')
    } else {
      await modelApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch { /* */ } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除模型 "${row.modelId}"？`, '删除确认', { type: 'warning' })
  try {
    await modelApi.delete(row.modelId)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* */ }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
</style>
