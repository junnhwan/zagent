<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">RAG 知识库</h2>
    </div>

    <el-row :gutter="20">
      <!-- Upload -->
      <el-col :span="12">
        <el-card>
          <template #header><span style="font-weight:600">上传文档</span></template>
          <el-form label-width="90px">
            <el-form-item label="知识标签">
              <el-input v-model="uploadTag" placeholder="如: product, hr, api" />
            </el-form-item>
            <el-form-item label="文件">
              <el-upload ref="uploadRef" :auto-upload="false" :limit="1" :on-change="handleFileChange" accept=".pdf,.doc,.docx,.txt,.md,.html">
                <el-button type="primary">选择文件</el-button>
                <template #tip><div class="el-upload__tip">支持 PDF、Word、TXT、MD、HTML</div></template>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="success" @click="handleUpload" :loading="uploading" :disabled="!uploadTag || !uploadFile">上传</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- Query -->
      <el-col :span="12">
        <el-card>
          <template #header><span style="font-weight:600">知识查询</span></template>
          <el-form label-width="90px">
            <el-form-item label="Client">
              <el-select v-model="queryClientId" filterable placeholder="选择 Client" style="width:100%">
                <el-option v-for="c in clients" :key="c.clientId" :label="`${c.clientId} (${c.clientName || ''})`" :value="c.clientId" />
              </el-select>
            </el-form-item>
            <el-form-item label="知识标签">
              <el-select v-model="queryTag" filterable clearable placeholder="可选，按标签过滤" style="width:100%">
                <el-option v-for="t in tags" :key="t.tag" :label="`${t.tag} (${t.count} 条)`" :value="t.tag" />
              </el-select>
            </el-form-item>
            <el-form-item label="问题">
              <el-input v-model="queryQuestion" type="textarea" :rows="3" placeholder="输入问题" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleQuery" :loading="querying" :disabled="!queryClientId || !queryQuestion">查询</el-button>
            </el-form-item>
          </el-form>
          <div v-if="queryResult" class="query-result">
            <el-divider />
            <div style="white-space:pre-wrap; line-height:1.7">{{ queryResult }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tags list -->
    <el-card style="margin-top:20px">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span style="font-weight:600">知识标签列表</span>
          <el-button @click="fetchTags" size="small">刷新</el-button>
        </div>
      </template>
      <el-table :data="tags" stripe>
        <el-table-column prop="tag" label="标签" />
        <el-table-column prop="count" label="文档数" width="120" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDeleteTag(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ragApi, clientApi } from '../api'

const clients = ref([])
const tags = ref([])

const fetchTags = async () => { try { tags.value = await ragApi.tags() || [] } catch { /* */ } }
onMounted(async () => {
  try { clients.value = await clientApi.list() || [] } catch { /* */ }
  fetchTags()
})

// Upload
const uploadTag = ref('')
const uploadFile = ref(null)
const uploading = ref(false)
const uploadRef = ref()

const handleFileChange = (file) => { uploadFile.value = file.raw }

const handleUpload = async () => {
  if (!uploadTag.value || !uploadFile.value) return
  uploading.value = true
  try {
    await ragApi.upload(uploadFile.value, uploadTag.value)
    ElMessage.success('上传成功')
    uploadFile.value = null
    if (uploadRef.value) uploadRef.value.clearFiles()
    fetchTags()
  } catch { /* */ } finally { uploading.value = false }
}

// Query
const queryClientId = ref('')
const queryTag = ref('')
const queryQuestion = ref('')
const querying = ref(false)
const queryResult = ref('')

const handleQuery = async () => {
  querying.value = true
  queryResult.value = ''
  try {
    queryResult.value = await ragApi.query({ clientId: queryClientId.value, question: queryQuestion.value, knowledgeTag: queryTag.value || undefined })
  } catch { /* */ } finally { querying.value = false }
}

// Delete tag
const handleDeleteTag = async (row) => {
  await ElMessageBox.confirm(`确认删除知识标签 "${row.tag}" 及其所有文档？`, '删除确认', { type: 'warning' })
  try { await ragApi.deleteTag(row.tag); ElMessage.success('删除成功'); fetchTags() } catch { /* */ }
}
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
</style>
