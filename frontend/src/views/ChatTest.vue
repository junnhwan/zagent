<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">对话测试</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="6">
        <el-form label-width="70px">
          <el-form-item label="Client">
            <el-select v-model="clientId" filterable placeholder="选择 Client" style="width:100%">
              <el-option v-for="c in clients" :key="c.clientId" :label="`${c.clientId} (${c.clientName || ''})`" :value="c.clientId" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>

    <div class="chat-container">
      <div class="chat-messages" ref="messagesRef">
        <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-message', msg.role]">
          <div class="message-role">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
          <div class="message-content" v-html="msg.content"></div>
        </div>
        <div v-if="streaming" class="chat-message assistant">
          <div class="message-role">AI</div>
          <div class="message-content streaming">{{ streamingText }}<span class="cursor">|</span></div>
        </div>
      </div>

      <div class="chat-input">
        <el-input v-model="input" placeholder="输入消息..." :disabled="streaming" @keyup.enter="sendMessage" size="large">
          <template #append>
            <el-button type="primary" @click="sendMessage" :disabled="!clientId || !input.trim() || streaming" :loading="streaming">发送</el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { clientApi } from '../api'

const clients = ref([])
const clientId = ref('')
const input = ref('')
const messages = ref([])
const streaming = ref(false)
const streamingText = ref('')
const messagesRef = ref()

onMounted(async () => { try { clients.value = await clientApi.list() || [] } catch { /* */ } })

const scrollToBottom = () => { nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight }) }

const sendMessage = () => {
  if (!clientId.value || !input.value.trim() || streaming.value) return
  const userMsg = input.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  input.value = ''
  scrollToBottom()

  streaming.value = true
  streamingText.value = ''

  const params = new URLSearchParams({ clientId: clientId.value, message: userMsg })
  const evtSource = new EventSource(`/api/chat/stream?${params}`)

  evtSource.onmessage = (event) => {
    streamingText.value += event.data
    scrollToBottom()
  }

  evtSource.onerror = () => {
    evtSource.close()
    if (streamingText.value) {
      messages.value.push({ role: 'assistant', content: streamingText.value })
    }
    streaming.value = false
    streamingText.value = ''
    scrollToBottom()
  }
}
</script>

<style scoped>
.page-container { padding: 0; display: flex; flex-direction: column; height: 100%; }
.page-header { margin-bottom: 12px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; }
.chat-container { flex: 1; display: flex; flex-direction: column; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; min-height: 500px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }
.chat-message { margin-bottom: 16px; }
.chat-message.user .message-content { background: #ecf5ff; border-radius: 8px; padding: 10px 14px; display: inline-block; max-width: 80%; }
.chat-message.assistant .message-content { background: #f4f4f5; border-radius: 8px; padding: 10px 14px; display: inline-block; max-width: 80%; white-space: pre-wrap; }
.message-role { font-size: 12px; color: #909399; margin-bottom: 4px; }
.streaming .cursor { animation: blink 0.8s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
.chat-input { padding: 12px; border-top: 1px solid #e4e7ed; }
</style>
