import { ref, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { sendMessageStream } from '../api/chat'
import { useChatStore } from '../stores/chat'

const STREAM_TIMEOUT = 60_000

export function useChatStream() {
  const chatStore = useChatStore()
  const { messages, sessionId, selectedApiKeyId } = storeToRefs(chatStore)
  const loading = ref(false)
  let abortController: AbortController | null = null
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  function abort() {
    if (timeoutId) {
      clearTimeout(timeoutId)
      timeoutId = null
    }
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }

  onUnmounted(abort)

  async function send(text: string) {
    if (!text.trim() || loading.value) return
    if (!selectedApiKeyId.value) return

    abort()
    abortController = new AbortController()

    messages.value.push({ role: 'user', content: text, time: new Date() })
    loading.value = true

    const assistantIdx = messages.value.length
    messages.value.push({
      role: 'assistant',
      content: '',
      sources: [],
      kagSources: [],
      time: new Date(),
    })

    timeoutId = setTimeout(() => abort(), STREAM_TIMEOUT)

    try {
      await sendMessageStream(
        {
          message: text,
          apiKeyId: selectedApiKeyId.value,
          sessionId: sessionId.value,
        },
        {
          onSession(sid) {
            sessionId.value = sid
          },
          onChunk(chunk) {
            messages.value[assistantIdx].content += chunk
          },
          onSources(sources) {
            messages.value[assistantIdx].sources = sources
          },
          onKagSources(kagSources) {
            messages.value[assistantIdx].kagSources = kagSources
          },
          onDone() {
            loading.value = false
          },
          onError(err) {
            messages.value[assistantIdx].content += '\n\n抱歉，发生了错误：' + err
            loading.value = false
          },
        },
        abortController.signal
      )
    } catch (e: any) {
      if (e.name === 'AbortError') {
        messages.value[assistantIdx].content += '\n\n请求已超时或被取消'
      } else {
        messages.value[assistantIdx].content =
          '抱歉，发生了错误：' + (e.message || '未知错误')
      }
      loading.value = false
    } finally {
      if (timeoutId) {
        clearTimeout(timeoutId)
        timeoutId = null
      }
    }
  }

  return { messages, loading, send, abort }
}
