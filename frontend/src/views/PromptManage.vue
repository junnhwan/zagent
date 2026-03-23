<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">提示词管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建提示词</el-button>
    </div>

    <el-table v-loading="loading" :data="prompts" stripe>
      <el-table-column prop="promptId" label="提示词 ID" width="200" />
      <el-table-column prop="promptName" label="名称" width="200" />
      <el-table-column prop="promptContent" label="内容" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" width="200" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑提示词' : '新建提示词'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="提示词 ID" prop="promptId">
          <el-input v-model="form.promptId" :disabled="isEdit" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="名称" prop="promptName">
          <el-input v-model="form.promptName" placeholder="提示词名称" />
        </el-form-item>
        <el-form-item label="内容" prop="promptContent">
          <el-input
            v-model="form.promptContent"
            type="textarea"
            :rows="12"
            placeholder="系统提示词内容..."
            class="prompt-editor"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述" />
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
import { promptApi } from '../api'

const loading = ref(false)
const prompts = ref([])

const fetchPrompts = async () => {
  loading.value = true
  try { prompts.value = await promptApi.list() || [] } catch { /* */ } finally { loading.value = false }
}

onMounted(fetchPrompts)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = ref({ promptId: '', promptName: '', promptContent: '', description: '', status: 1 })

const rules = {
  promptId: [{ required: true, message: '请输入提示词 ID', trigger: 'blur' }],
  promptName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  promptContent: [{ required: true, message: '请输入提示词内容', trigger: 'blur' }]
}

const openDialog = (row) => {
  isEdit.value = !!row
  form.value = row ? { ...row } : { promptId: '', promptName: '', promptContent: '', description: '', status: 1 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await promptApi.update(form.value.promptId, form.value)
      ElMessage.success('更新成功')
    } else {
      await promptApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchPrompts()
  } catch { /* */ } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除提示词 "${row.promptId}"？`, '删除确认', { type: 'warning' })
  try {
    await promptApi.delete(row.promptId)
    ElMessage.success('删除成功')
    fetchPrompts()
  } catch { /* */ }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
:deep(.prompt-editor textarea) {
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.6;
}
</style>
