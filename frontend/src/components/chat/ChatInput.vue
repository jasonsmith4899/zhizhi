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
    <el-input
      v-model="input"
      type="textarea"
      :rows="2"
      placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
      :disabled="loading"
      resize="none"
      @keydown="handleKeydown"
    />
    <el-button
      type="primary"
      :loading="loading"
      style="margin-left: 12px; height: 56px; min-width: 80px"
      @click="handleSend"
    >
      发送
    </el-button>
  </div>
</template>

<style scoped>
.chat-input {
  display: flex;
  align-items: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #e6e6e6;
}
</style>
