<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  loading: boolean
}>()

const emit = defineEmits<{
  send: [message: string]
}>()

const input = ref('')

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleSend() {
  const text = input.value.trim()
  if (!text) return
  emit('send', text)
  input.value = ''
}

function clear() {
  input.value = ''
}

defineExpose({ clear })
</script>

<template>
  <div class="chat-input">
    <div class="input-wrapper">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
        :disabled="loading"
        resize="none"
        class="message-input"
        @keydown="handleKeydown"
      />
      <div class="input-decoration">
        <div class="decoration-line"></div>
      </div>
    </div>
    <el-button
      type="primary"
      :loading="loading"
      class="send-btn"
      @click="handleSend"
    >
      <span class="btn-content">
        <span class="btn-icon">↑</span>
        <span class="btn-text">发送</span>
      </span>
      <div class="btn-glow"></div>
    </el-button>
  </div>
</template>

<style scoped>
.chat-input {
  display: flex;
  align-items: flex-end;
  padding: 20px 24px;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border-top: 1px solid var(--border-color);
  position: relative;
  z-index: 10;
}

.chat-input::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-primary), var(--color-neon-blue), transparent);
  opacity: 0.3;
}

.input-wrapper {
  flex: 1;
  position: relative;
}

.message-input :deep(.el-textarea__inner) {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  color: var(--text-primary);
  font-family: 'Exo 2', sans-serif;
  font-size: 14px;
  padding: 16px 20px;
  transition: all var(--transition-normal);
  box-shadow: none;
}

.message-input :deep(.el-textarea__inner:hover) {
  border-color: var(--color-primary);
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: var(--color-neon-blue);
  box-shadow: 0 0 0 3px rgba(0, 212, 255, 0.1);
}

.message-input :deep(.el-textarea__inner::placeholder) {
  color: var(--text-muted);
}

.input-decoration {
  position: absolute;
  bottom: 0;
  left: 20px;
  right: 20px;
  height: 2px;
  overflow: hidden;
}

.decoration-line {
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--color-neon-blue), transparent);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.message-input:focus-within .decoration-line {
  opacity: 0.5;
}

.send-btn {
  height: 56px;
  min-width: 100px;
  margin-left: 16px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  border: none;
  border-radius: var(--radius-lg);
  position: relative;
  overflow: hidden;
  transition: all var(--transition-normal);
}

.send-btn:hover {
  background: linear-gradient(135deg, var(--color-primary-light), var(--color-primary));
  box-shadow: 0 0 30px rgba(0, 102, 255, 0.4);
  transform: translateY(-2px);
}

.btn-content {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.btn-icon {
  font-size: 18px;
  font-weight: 700;
}

.btn-text {
  font-family: 'Rajdhani', sans-serif;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.send-btn:hover .btn-glow {
  left: 100%;
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-input {
    padding: 12px 16px;
  }

  .send-btn {
    height: 48px;
    min-width: 70px;
  }

  .btn-text {
    display: none;
  }
}
</style>
