<script setup lang="ts">
defineProps<{
  apiKeys: any[]
  selectedApiKeyId?: number
}>()

defineEmits<{
  'update:selectedApiKeyId': [value: number | undefined]
  newChat: []
}>()
</script>

<template>
  <div class="chat-header">
    <div class="header-left">
      <div class="header-title">
        <div class="title-icon">
          <div class="icon-pulse"></div>
          <span>AI</span>
        </div>
        <span class="title-text">智能对话</span>
      </div>
      <el-select
        :model-value="selectedApiKeyId"
        placeholder="请选择 API Key"
        size="small"
        class="api-select"
        @update:model-value="$emit('update:selectedApiKeyId', $event)"
      >
        <el-option
          v-for="key in apiKeys"
          :key="key.id"
          :label="key.name"
          :value="key.id"
        />
      </el-select>
      <el-tag v-if="selectedApiKeyId" type="success" size="small" class="rag-tag">
        <span class="tag-dot"></span>
        RAG 模式
      </el-tag>
    </div>
    <el-button size="small" class="new-chat-btn" @click="$emit('newChat')">
      <span class="btn-icon">+</span>
      新对话
    </el-button>
  </div>
</template>

<style scoped>
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-color);
  position: relative;
  z-index: 10;
}

.chat-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-primary), var(--color-neon-blue), transparent);
  opacity: 0.5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-neon-blue));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.icon-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 16px;
  border: 2px solid var(--color-neon-blue);
  opacity: 0;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.1);
  }
}

.title-icon span {
  font-family: 'Orbitron', monospace;
  font-size: 14px;
  font-weight: 800;
  color: white;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
}

.title-text {
  font-family: 'Rajdhani', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.api-select {
  width: 220px;
}

.api-select :deep(.el-input__wrapper) {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: none;
}

.api-select :deep(.el-input__wrapper:hover) {
  border-color: var(--color-primary);
}

.api-select :deep(.el-input__wrapper.is-focus) {
  border-color: var(--color-neon-blue);
  box-shadow: 0 0 0 3px rgba(0, 212, 255, 0.1);
}

.api-select :deep(.el-input__inner) {
  color: var(--text-primary);
}

.rag-tag {
  background: rgba(0, 255, 136, 0.1);
  border: 1px solid rgba(0, 255, 136, 0.3);
  color: #00ff88;
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tag-dot {
  width: 6px;
  height: 6px;
  background: #00ff88;
  border-radius: 50%;
  box-shadow: 0 0 10px rgba(0, 255, 136, 0.5);
  animation: blink 2s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.new-chat-btn {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all var(--transition-normal);
  display: flex;
  align-items: center;
  gap: 6px;
}

.new-chat-btn:hover {
  background: rgba(0, 102, 255, 0.1);
  border-color: var(--color-primary);
  color: var(--color-neon-blue);
  box-shadow: 0 0 20px rgba(0, 102, 255, 0.2);
}

.btn-icon {
  font-size: 16px;
  font-weight: 700;
}
</style>
