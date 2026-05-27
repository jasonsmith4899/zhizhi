<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import MessageBubble from './MessageBubble.vue'

interface Msg {
  role: string
  content: string
  sources?: any[]
  time: Date
}

const props = defineProps<{
  messages: Msg[]
  loading: boolean
}>()

const emit = defineEmits<{
  openPreview: [docId: number]
}>()

const chatContainer = ref<HTMLElement | null>(null)

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, scrollToBottom)
watch(() => props.messages[props.messages.length - 1]?.content, scrollToBottom)

defineExpose({ scrollToBottom })
</script>

<template>
  <div ref="chatContainer" class="chat-messages">
    <div v-if="messages.length === 0" class="empty-chat">
      <div style="font-size: 48px; margin-bottom: 16px">🤖</div>
      <div style="font-size: 18px; color: #606266; margin-bottom: 8px">
        欢迎使用智知 AI 知识库
      </div>
      <div style="font-size: 14px; color: #909399">
        选择一个知识库开启 RAG 问答，或直接对话
      </div>
    </div>

    <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role">
      <div class="msg-avatar">
        {{ msg.role === 'user' ? '👤' : '🤖' }}
      </div>
      <MessageBubble
        :message="msg"
        :is-streaming="loading && idx === messages.length - 1 && msg.role === 'assistant'"
        @open-preview="emit('openPreview', $event)"
      />
    </div>

    <div v-if="loading && messages[messages.length - 1]?.content === ''" class="msg-row assistant">
      <div class="msg-avatar">🤖</div>
      <div class="msg-bubble">
        <div class="typing-indicator">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.msg-row {
  display: flex;
  margin-bottom: 20px;
}
.msg-row.user {
  flex-direction: row-reverse;
}
.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.msg-row.user .msg-avatar {
  margin-left: 12px;
}
.msg-row.assistant .msg-avatar {
  margin-right: 12px;
}
.msg-row.user :deep(.msg-bubble) {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 4px;
}
.msg-row.assistant :deep(.msg-bubble) {
  background: #f5f7fa;
  color: #303133;
  border-top-left-radius: 4px;
}
.msg-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
}
.typing-indicator {
  display: flex;
  gap: 4px;
}
.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
  animation: bounce 1.4s infinite ease-in-out;
}
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
