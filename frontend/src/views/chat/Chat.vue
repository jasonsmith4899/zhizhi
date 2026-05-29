<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { getKnowledgeBases, getDocumentPreview } from '../../api/knowledge'
import { useChatStore } from '../../stores/chat'
import { useChatStream } from '../../composables/useChatStream'
import ChatHeader from '../../components/chat/ChatHeader.vue'
import MessageList from '../../components/chat/MessageList.vue'
import ChatInput from '../../components/chat/ChatInput.vue'
import SourcePreview from '../../components/chat/SourcePreview.vue'

const chatStore = useChatStore()
const { selectedKbId } = storeToRefs(chatStore)
const { messages, loading, send, abort } = useChatStream()

const knowledgeBases = ref<any[]>([])
const chatInputRef = ref<InstanceType<typeof ChatInput> | null>(null)

// 文档预览
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref<any>(null)

onMounted(async () => {
  try {
    const res = await getKnowledgeBases()
    knowledgeBases.value = (res as any).data || []
  } catch {}
})

onUnmounted(() => {
  abort()
})

function handleSend(text: string) {
  if (!selectedKbId.value) {
    ElMessage.warning('请先选择知识库')
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
      :knowledge-bases="knowledgeBases"
      :selected-kb-id="selectedKbId"
      @update:selected-kb-id="selectedKbId = $event"
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
  height: calc(100vh - 120px);
  background: #fff;
}
</style>
