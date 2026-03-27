<template>
  <div class="studio-layout">
    <aside class="studio-aside" :class="{ collapsed: isCollapsed }">
      <div class="brand" @click="isCollapsed = !isCollapsed">
        <div class="brand-icon">ZA</div>
        <div v-show="!isCollapsed" class="brand-text">
          <h1>zagent</h1>
          <p>工作台</p>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        :collapse="isCollapsed"
        class="menu"
      >
        <el-menu-item index="/overview">
          <el-icon><DataBoard /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/playground">
          <el-icon><Promotion /></el-icon>
          <template #title>运行台</template>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Reading /></el-icon>
          <template #title>知识库</template>
        </el-menu-item>
        <el-menu-item index="/observability">
          <el-icon><Histogram /></el-icon>
          <template #title>运行记录</template>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>配置</template>
        </el-menu-item>
      </el-menu>
    </aside>
    <main class="studio-main">
      <header class="studio-header">
        <div>
          <h2>{{ route.meta.title || '工作台' }}</h2>
          <p>输入、运行、查看结果与配置入口</p>
        </div>
        <div class="header-actions">
          <el-button text @click="$router.push('/playground')">开始运行</el-button>
          <el-button text @click="$router.push('/settings')">打开配置</el-button>
          <el-tooltip content="刷新缓存" placement="bottom">
            <el-button :icon="Refresh" circle :loading="cacheLoading" @click="handleInvalidateAll" />
          </el-tooltip>
        </div>
      </header>
      <section class="studio-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { cacheApi } from '../api'

const route = useRoute()
const isCollapsed = ref(false)
const cacheLoading = ref(false)
const rootMenus = ['/overview', '/playground', '/knowledge', '/observability', '/settings']

const activeMenu = computed(() => {
  const first = `/${route.path.split('/').filter(Boolean)[0] || 'overview'}`
  return rootMenus.includes(first) ? first : '/overview'
})

const handleInvalidateAll = async () => {
  cacheLoading.value = true
  try {
    await cacheApi.invalidateAll()
    ElMessage.success('缓存刷新成功')
  } catch {
    ElMessage.error('缓存刷新失败，请稍后重试')
  } finally {
    cacheLoading.value = false
  }
}
</script>

<style scoped>
.studio-layout {
  display: grid;
  grid-template-columns: 224px 1fr;
  min-height: 100vh;
  background: #f6f8fb;
}

.studio-aside {
  width: 224px;
  border-right: 1px solid #e6ebf3;
  background: #ffffff;
  transition: width 0.2s ease;
}

.studio-aside.collapsed {
  width: 64px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 16px 14px;
  cursor: pointer;
}

.brand-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #2f5bea;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.brand-text h1 {
  margin: 0;
  font-size: 18px;
  line-height: 1.1;
}

.brand-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #6d7891;
}

.menu {
  border-right: none;
  background: transparent;
  padding: 8px 10px;
}

.studio-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.studio-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #e6ebf3;
  background: #ffffff;
}

.studio-header h2 {
  margin: 0;
  font-size: 18px;
}

.studio-header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6d7891;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.studio-content {
  padding: 20px 24px 24px;
  overflow: auto;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  border-radius: 10px;
  margin-bottom: 6px;
}

:deep(.el-menu-item.is-active) {
  background: #eef4ff;
  color: #2f5bea;
}

@media (max-width: 992px) {
  .studio-layout {
    grid-template-columns: 76px 1fr;
  }

  .studio-aside {
    width: 76px;
  }

  .brand-text,
  .studio-header p {
    display: none;
  }
}
</style>
