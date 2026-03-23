<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">Advisor 管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建 Advisor</el-button>
    </div>

    <el-table v-loading="loading" :data="advisors" stripe>
      <el-table-column prop="advisorId" label="Advisor ID" width="200" />
      <el-table-column prop="advisorName" label="名称" width="200" />
      <el-table-column prop="advisorType" label="类型" width="150">
        <template #default="{ row }">
          <el-tag size="small">{{ row.advisorType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderNum" label="排序" width="80" />
      <el-table-column prop="extParam" label="扩展参数" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 Advisor' : '新建 Advisor'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="Advisor ID" prop="advisorId">
          <el-input v-model="form.advisorId" :disabled="isEdit" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="名称" prop="advisorName">
          <el-input v-model="form.advisorName" placeholder="Advisor 名称" />
        </el-form-item>
        <el-form-item label="类型" prop="advisorType">
          <el-input v-model="form.advisorType" placeholder="如: MessageChatMemoryAdvisor, RagContextAdvisor" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="扩展参数">
          <el-input
            v-model="form.extParam"
            type="textarea"
            :rows="6"
            placeholder='JSON 格式, 如: {"chatMemoryRetrieveSize": 10}'
            class="json-editor"
          />
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
import { advisorApi } from '../api'

const loading = ref(false)
const advisors = ref([])

const fetchAdvisors = async () => {
  loading.value = true
  try { advisors.value = await advisorApi.list() || [] } catch { /* */ } finally { loading.value = false }
}

onMounted(fetchAdvisors)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = ref({ advisorId: '', advisorName: '', advisorType: '', orderNum: 0, extParam: '', status: 1 })

const rules = {
  advisorId: [{ required: true, message: '请输入 Advisor ID', trigger: 'blur' }],
  advisorName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  advisorType: [{ required: true, message: '请输入类型', trigger: 'blur' }]
}

const openDialog = (row) => {
  isEdit.value = !!row
  form.value = row
    ? { ...row }
    : { advisorId: '', advisorName: '', advisorType: '', orderNum: 0, extParam: '', status: 1 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await advisorApi.update(form.value.advisorId, form.value)
      ElMessage.success('更新成功')
    } else {
      await advisorApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchAdvisors()
  } catch { /* */ } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除 Advisor "${row.advisorId}"？`, '删除确认', { type: 'warning' })
  try {
    await advisorApi.delete(row.advisorId)
    ElMessage.success('删除成功')
    fetchAdvisors()
  } catch { /* */ }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
:deep(.json-editor textarea) {
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
}
</style>
