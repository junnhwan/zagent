<template>
  <div class="page">
    <section class="section">
      <h3>Tools</h3>
      <p class="section-desc">工具层用于连接外部能力，当前通过 MCP 配置页管理。</p>
      <el-card class="card">
        <h4>MCP 工具接入</h4>
        <p>在 Settings 中维护 MCP 服务、运行状态与可调用能力。</p>
        <div class="actions">
          <el-button type="primary" @click="$router.push('/settings/mcps')">打开 MCP 配置</el-button>
          <el-button @click="$router.push('/settings')">返回 Settings</el-button>
        </div>
      </el-card>
    </section>
  </div>
</template>
