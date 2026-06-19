<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { getDocumentPreview } from '../../api/knowledge'
import { listApiKeys } from '../../api/apiKey'
import { useChatStore } from '../../stores/chat'
import { useChatStream } from '../../composables/useChatStream'
import ChatHeader from '../../components/chat/ChatHeader.vue'
import MessageList from '../../components/chat/MessageList.vue'
import ChatInput from '../../components/chat/ChatInput.vue'
import SourcePreview from '../../components/chat/SourcePreview.vue'

const chatStore = useChatStore()
const { selectedApiKeyId } = storeToRefs(chatStore)
const { messages, loading, send, abort } = useChatStream()

const apiKeys = ref<any[]>([])
const chatInputRef = ref<InstanceType<typeof ChatInput> | null>(null)

// 文档预览
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref<any>(null)

onMounted(async () => {
  try {
    const res = await listApiKeys()
    apiKeys.value = (res as any).data || []
  } catch {}
})

onUnmounted(() => {
  abort()
})

function handleSend(text: string) {
  if (!selectedApiKeyId.value) {
    ElMessage.warning('请先选择 API Key')
    return
  }
  send(text)
}

function newChat() {
  chatStore.newChat()
  chatInputRef.value?.clear()
}

async function openPreview(docId: number) {
  previewLoading.value = true
  previewVisible.value = true
  previewData.value = null
  try {
    const res = (await getDocumentPreview(docId)) as any
    previewData.value = res.data
  } catch {
    previewData.value = null
  } finally {
    previewLoading.value = false
  }
}
</script>

<template>
  <div class="chat-page">
    <ChatHeader
      :api-keys="apiKeys"
      :selected-api-key-id="selectedApiKeyId"
      @update:selected-api-key-id="selectedApiKeyId = $event"
      @new-chat="newChat"
    />
    <MessageList
      :messages="messages"
      :loading="loading"
      @open-preview="openPreview"
    />
    <ChatInput
      ref="chatInputRef"
      :loading="loading"
      @send="handleSend"
    />
    <SourcePreview
      v-model:preview-visible="previewVisible"
      :preview-loading="previewLoading"
      :preview-data="previewData"
    />
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--header-height));
  background: var(--bg-dark);
  position: relative;
  overflow: hidden;
}

.chat-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(0, 102, 255, 0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(0, 212, 255, 0.03) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.chat-page::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(0, 102, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 102, 255, 0.02) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
  z-index: 0;
}
</style>
