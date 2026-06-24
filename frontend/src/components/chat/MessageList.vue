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
      <div class="empty-icon">
        <div class="icon-glow"></div>
        <div class="icon-hex">
          <span>AI</span>
        </div>
      </div>
      <div class="empty-title">欢迎使用智知 AI 知识库</div>
      <div class="empty-subtitle">选择一个知识库开启 RAG 问答，或直接对话</div>
      <div class="empty-decoration">
        <div class="decoration-line line-1"></div>
        <div class="decoration-line line-2"></div>
        <div class="decoration-line line-3"></div>
      </div>
    </div>

    <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role">
      <MessageBubble
        :message="msg"
        :is-streaming="loading && idx === messages.length - 1 && msg.role === 'assistant'"
        @open-preview="emit('openPreview', $event)"
      />
    </div>

    <!-- typing indicator 已移入 MessageBubble，避免重复气泡 -->
  </div>
</template>

<style scoped>
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  position: relative;
  z-index: 1;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;
}

.empty-icon {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 32px;
}

.icon-glow {
  position: absolute;
  inset: -20px;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse-glow 3s ease-in-out infinite;
}

.icon-hex {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-neon-blue));
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
  animation: float 6s ease-in-out infinite;
}

.icon-hex span {
  font-family: 'Orbitron', monospace;
  font-size: 28px;
  font-weight: 800;
  color: var(--color-text-on-accent);
  text-shadow: 0 0 20px var(--overlay-white-50);
}

.empty-title {
  font-family: 'Rajdhani', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
  text-align: center;
}

.empty-subtitle {
  font-family: 'Exo 2', sans-serif;
  font-size: 16px;
  color: var(--text-secondary);
  text-align: center;
  max-width: 400px;
}

.empty-decoration {
  display: flex;
  gap: 12px;
  margin-top: 40px;
}

.decoration-line {
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-neon-blue), transparent);
  animation: line-pulse 2s ease-in-out infinite;
}

.line-1 {
  width: 60px;
  animation-delay: 0s;
}

.line-2 {
  width: 80px;
  animation-delay: 0.3s;
}

.line-3 {
  width: 40px;
  animation-delay: 0.6s;
}

@keyframes line-pulse {
  0%, 100% {
    opacity: 0.3;
    transform: scaleX(1);
  }
  50% {
    opacity: 0.8;
    transform: scaleX(1.2);
  }
}

.msg-row {
  margin-bottom: 8px;
}

.msg-row.user {
  display: flex;
  justify-content: flex-end;
}

.msg-row.assistant {
  display: flex;
  justify-content: flex-start;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-10px) rotate(5deg);
  }
}

@keyframes pulse-glow {
  0%, 100% {
    opacity: 0.3;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.1);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-messages {
    padding: 16px;
  }

  .empty-icon {
    width: 80px;
    height: 80px;
    margin-bottom: 24px;
  }

  .icon-hex span {
    font-size: 20px;
  }

  .empty-title {
    font-size: 22px;
  }

  .empty-subtitle {
    font-size: 14px;
  }
}
</style>
