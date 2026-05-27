import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

interface Msg {
  role: string
  content: string
  sources?: any[]
  time: Date
}

const STORAGE_KEY = 'zhizhi-chat-state'
const MAX_PERSISTED_MESSAGES = 50

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Msg[]>([])
  const sessionId = ref<string | undefined>(undefined)
  const selectedKbId = ref<number | undefined>(undefined)
  let skipSave = false

  // 从 localStorage 恢复状态
  function loadFromStorage() {
    try {
      skipSave = true
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        const data = JSON.parse(raw)
        messages.value = (data.messages || []).map((m: any) => ({
          ...m,
          time: new Date(m.time),
        }))
        sessionId.value = data.sessionId || undefined
        selectedKbId.value = data.selectedKbId || undefined
      }
    } catch (e) {
      console.error('Failed to load chat state from storage:', e)
    } finally {
      skipSave = false
    }
  }

  // F5: 限制持久化消息数量，F7: 给用户提示
  function saveToStorage() {
    if (skipSave) return
    try {
      const messagesToSave = messages.value.slice(-MAX_PERSISTED_MESSAGES)
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          messages: messagesToSave,
          sessionId: sessionId.value,
          selectedKbId: selectedKbId.value,
        })
      )
    } catch (e: any) {
      if (e.name === 'QuotaExceededError') {
        ElMessage.warning('本地存储空间不足，部分历史消息可能未保存')
      }
      console.error('Failed to save chat state to storage:', e)
    }
  }

  // 监听变化自动持久化
  watch([messages, sessionId, selectedKbId], saveToStorage, { deep: true })

  // F7: 跨标签页同步
  window.addEventListener('storage', (e) => {
    if (e.key !== STORAGE_KEY || !e.newValue) return
    try {
      skipSave = true
      const data = JSON.parse(e.newValue)
      messages.value = (data.messages || []).map((m: any) => ({
        ...m,
        time: new Date(m.time),
      }))
      sessionId.value = data.sessionId || undefined
      selectedKbId.value = data.selectedKbId || undefined
    } catch (err) {
      console.error('Failed to sync chat state across tabs:', err)
    } finally {
      skipSave = false
    }
  })

  // 初始化时加载
  loadFromStorage()

  function newChat() {
    messages.value = []
    sessionId.value = undefined
  }

  return {
    messages,
    sessionId,
    selectedKbId,
    newChat,
  }
})
