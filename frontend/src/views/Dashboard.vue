<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>

    <!-- Stats cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card" @click="$router.push(card.route)">
          <div class="stat-card-body">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="28"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick links -->
    <el-card class="quick-links-card">
      <template #header>
        <span>快捷入口</span>
      </template>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :md="6" v-for="link in quickLinks" :key="link.route">
          <div class="quick-link" @click="$router.push(link.route)">
            <el-icon :size="20"><component :is="link.icon" /></el-icon>
            <span>{{ link.label }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { agentApi, clientApi, modelApi, mcpApi } from '../api'

const statCards = ref([
  { label: 'Agents', value: '-', color: '#409eff22', icon: 'Cpu', route: '/agents' },
  { label: 'Clients', value: '-', color: '#67c23a22', icon: 'Connection', route: '/clients' },
  { label: '模型', value: '-', color: '#e6a23c22', icon: 'Coin', route: '/models' },
  { label: 'MCP 工具', value: '-', color: '#f56c6c22', icon: 'SetUp', route: '/mcps' }
])

const quickLinks = [
  { label: 'Agent 管理', icon: 'Cpu', route: '/agents' },
  { label: 'Client 管理', icon: 'Connection', route: '/clients' },
  { label: '模型管理', icon: 'Coin', route: '/models' },
  { label: 'API 管理', icon: 'Link', route: '/apis' },
  { label: '提示词管理', icon: 'EditPen', route: '/prompts' },
  { label: 'Advisor 管理', icon: 'MagicStick', route: '/advisors' },
  { label: 'MCP 工具', icon: 'SetUp', route: '/mcps' },
  { label: '对话测试', icon: 'ChatDotRound', route: '/chat' },
  { label: 'Agent 测试', icon: 'VideoPlay', route: '/agent-test' },
  { label: 'RAG 管理', icon: 'Files', route: '/rag' }
]

onMounted(async () => {
  try {
    const [agents, clients, models, mcps] = await Promise.allSettled([
      agentApi.list(),
      clientApi.list(),
      modelApi.list(),
      mcpApi.list()
    ])
    if (agents.status === 'fulfilled') statCards.value[0].value = Array.isArray(agents.value) ? agents.value.length : 0
    if (clients.status === 'fulfilled') statCards.value[1].value = Array.isArray(clients.value) ? clients.value.length : 0
    if (models.status === 'fulfilled') statCards.value[2].value = Array.isArray(models.value) ? models.value.length : 0
    if (mcps.status === 'fulfilled') statCards.value[3].value = Array.isArray(mcps.value) ? mcps.value.length : 0
  } catch {
    // cards stay at '-'
  }
})
</script>

<style scoped>
.page-title {
  margin: 0 0 20px;
  font-size: 22px;
  font-weight: 600;
}
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  cursor: pointer;
  margin-bottom: 12px;
}
.stat-card-body {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  color: #909399;
  font-size: 14px;
}
.quick-links-card {
  margin-bottom: 20px;
}
.quick-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  color: #303133;
}
.quick-link:hover {
  background: #f0f2f5;
  color: #409eff;
}
</style>
