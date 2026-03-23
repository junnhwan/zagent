<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo-area" @click="isCollapsed = !isCollapsed">
        <el-icon :size="24"><Monitor /></el-icon>
        <span v-show="!isCollapsed" class="logo-text">ZAgent</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        :collapse="isCollapsed"
        background-color="#1d1e2c"
        text-color="#a3a6b4"
        active-text-color="#409eff"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>

        <el-sub-menu index="resource">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>资源管理</span>
          </template>
          <el-menu-item index="/agents">
            <el-icon><Cpu /></el-icon>
            <template #title>Agent 管理</template>
          </el-menu-item>
          <el-menu-item index="/clients">
            <el-icon><Connection /></el-icon>
            <template #title>Client 管理</template>
          </el-menu-item>
          <el-menu-item index="/models">
            <el-icon><Coin /></el-icon>
            <template #title>模型管理</template>
          </el-menu-item>
          <el-menu-item index="/apis">
            <el-icon><Link /></el-icon>
            <template #title>API 管理</template>
          </el-menu-item>
          <el-menu-item index="/prompts">
            <el-icon><EditPen /></el-icon>
            <template #title>提示词管理</template>
          </el-menu-item>
          <el-menu-item index="/advisors">
            <el-icon><MagicStick /></el-icon>
            <template #title>Advisor 管理</template>
          </el-menu-item>
          <el-menu-item index="/mcps">
            <el-icon><SetUp /></el-icon>
            <template #title>MCP 工具</template>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="testing">
          <template #title>
            <el-icon><Promotion /></el-icon>
            <span>测试中心</span>
          </template>
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>对话测试</template>
          </el-menu-item>
          <el-menu-item index="/agent-test">
            <el-icon><VideoPlay /></el-icon>
            <template #title>Agent 测试</template>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/rag">
          <el-icon><Files /></el-icon>
          <template #title>RAG 管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- Main -->
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>ZAgent Admin</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="刷新所有缓存" placement="bottom">
            <el-button :icon="Refresh" circle @click="handleInvalidateAll" :loading="cacheLoading" />
          </el-tooltip>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { cacheApi } from '../api'

const route = useRoute()
const isCollapsed = ref(false)
const cacheLoading = ref(false)

const handleInvalidateAll = async () => {
  cacheLoading.value = true
  try {
    await cacheApi.invalidateAll()
    ElMessage.success('缓存已全部刷新')
  } catch {
    // interceptor handles the error
  } finally {
    cacheLoading.value = false
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background-color: #1d1e2c;
  transition: width 0.3s;
  overflow-x: hidden;
}
.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  cursor: pointer;
  color: #409eff;
  font-weight: 700;
  border-bottom: 1px solid #2d2e3e;
}
.logo-text {
  font-size: 18px;
  white-space: nowrap;
}
.sidebar-menu {
  border-right: none;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}
.layout-main {
  background: #f5f7fa;
  min-height: 0;
  overflow-y: auto;
}
</style>
