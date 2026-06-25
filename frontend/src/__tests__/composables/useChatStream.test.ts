import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/chat', () => ({
  sendMessageStream: vi.fn(),
}))

// Mock element-plus to avoid import issues
vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    error: vi.fn(),
  },
}))

import { useChatStream } from '@/composables/useChatStream'
import { useChatStore } from '@/stores/chat'
import { sendMessageStream } from '@/api/chat'

const mockedSendMessageStream = vi.mocked(sendMessageStream)

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  vi.clearAllMocks()
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('composables/useChatStream', () => {
  function setupStoreWithApiKey(apiKeyId?: number) {
    const store = useChatStore()
    store.selectedApiKeyId = apiKeyId
    return store
  }

  describe('send', () => {
    it('should not send when text is empty', async () => {
      setupStoreWithApiKey(1)
      const { send } = useChatStream()

      await send('')
      expect(mockedSendMessageStream).not.toHaveBeenCalled()
    })

    it('should not send when text is whitespace only', async () => {
      setupStoreWithApiKey(1)
      const { send } = useChatStream()

      await send('   ')
      expect(mockedSendMessageStream).not.toHaveBeenCalled()
    })

    it('should not send when no API key is selected', async () => {
      setupStoreWithApiKey(undefined)
      const { send } = useChatStream()

      await send('hello')
      expect(mockedSendMessageStream).not.toHaveBeenCalled()
    })

    it('should push user message and create assistant placeholder', async () => {
      setupStoreWithApiKey(1)
      mockedSendMessageStream.mockImplementation(async (_data, callbacks) => {
        callbacks.onChunk('Hello')
        callbacks.onDone()
      })

      const { send, messages } = useChatStream()

      await send('test message')

      expect(messages.value).toHaveLength(2)
      expect(messages.value[0]).toMatchObject({
        role: 'user',
        content: 'test message',
      })
      expect(messages.value[1]).toMatchObject({
        role: 'assistant',
        content: 'Hello',
        sources: [],
        kagSources: [],
      })
    })

    it('should call sendMessageStream with correct parameters', async () => {
      const store = setupStoreWithApiKey(5)
      store.sessionId = 'existing-session'

      mockedSendMessageStream.mockImplementation(async (_data, callbacks) => {
        callbacks.onDone()
      })

      const { send } = useChatStream()
      await send('hello')

      expect(mockedSendMessageStream).toHaveBeenCalledWith(
        {
          message: 'hello',
          apiKeyId: 5,
          sessionId: 'existing-session',
        },
        expect.objectContaining({
          onSession: expect.any(Function),
          onChunk: expect.any(Function),
          onSources: expect.any(Function),
          onKagSources: expect.any(Function),
          onDone: expect.any(Function),
          onError: expect.any(Function),
        }),
        expect.any(AbortSignal),
      )
    })

    it('should update sessionId when onSession callback fires', async () => {
      setupStoreWithApiKey(1)
      mockedSendMessageStream.mockImplementation(async (_data, callbacks) => {
        callbacks.onSession('new-session-id')
        callbacks.onDone()
      })

      const { send } = useChatStream()
      const store = useChatStore()

      await send('hello')
      expect(store.sessionId).toBe('new-session-id')
    })

    it('should accumulate chunks from onChunk callbacks', async () => {
      setupStoreWithApiKey(1)
      mockedSendMessageStream.mockImplementation(async (_data, callbacks) => {
        callbacks.onChunk('Hello')
        callbacks.onChunk(' ')
        callbacks.onChunk('World')
        callbacks.onDone()
      })

      const { send, messages } = useChatStream()
      await send('test')

      expect(messages.value[1].content).toBe('Hello World')
    })

    it('should set sources when onSources callback fires', async () => {
      setupStoreWithApiKey(1)
      const sources = [{ documentId: 1, documentName: 'doc.pdf', content: 'snippet', score: 0.9 }]
      mockedSendMessageStream.mockImplementation(async (_data, callbacks) => {
        callbacks.onSources(sources)
        callbacks.onDone()
      })

      const { send, messages } = useChatStream()
      await send('test')

      expect(messages.value[1].sources).toEqual(sources)
    })

    it('should set kagSources when onKagSources callback fires', async () => {
      setupStoreWithApiKey(1)
      const kagSources = [{ name: 'entity', type: 'PERSON' }]
      mockedSendMessageStream.mockImplementation(async (_data, callbacks) => {
        callbacks.onKagSources(kagSources)
        callbacks.onDone()
      })

      const { send, messages } = useChatStream()
      await send('test')

      expect(messages.value[1].kagSources).toEqual(kagSources)
    })

    it('should set loading to false when onDone fires', async () => {
      setupStoreWithApiKey(1)
      mockedSendMessageStream.mockImplementation(async (_data, callbacks) => {
        callbacks.onDone()
      })

      const { send, loading } = useChatStream()
      expect(loading.value).toBe(false)

      // loading should become true during send
      const sendPromise = send('test')
      // We need to advance microtasks
      await sendPromise
      expect(loading.value).toBe(false)
    })

    it('should append error to assistant message on error callback', async () => {
      setupStoreWithApiKey(1)
      mockedSendMessageStream.mockImplementation(async (_data, callbacks) => {
        callbacks.onError('Something went wrong')
      })

      const { send, messages, loading } = useChatStream()
      await send('test')

      expect(messages.value[1].content).toContain('抱歉，发生了错误：Something went wrong')
      expect(loading.value).toBe(false)
    })

    it('should handle stream exceptions gracefully', async () => {
      setupStoreWithApiKey(1)
      mockedSendMessageStream.mockRejectedValue(new Error('Network failure'))

      const { send, messages, loading } = useChatStream()
      await send('test')

      expect(messages.value[1].content).toContain('抱歉，发生了错误：Network failure')
      expect(loading.value).toBe(false)
    })

    it('should handle AbortError by appending cancellation message', async () => {
      setupStoreWithApiKey(1)
      const abortError = new Error('The operation was aborted')
      abortError.name = 'AbortError'
      mockedSendMessageStream.mockRejectedValue(abortError)

      const { send, messages } = useChatStream()
      await send('test')

      expect(messages.value[1].content).toContain('请求已超时或被取消')
    })

    it('should set loading to false after stream exception', async () => {
      setupStoreWithApiKey(1)
      mockedSendMessageStream.mockRejectedValue(new Error('fail'))

      const { send, loading } = useChatStream()
      await send('test')

      expect(loading.value).toBe(false)
    })

    it('should not allow concurrent sends', async () => {
      setupStoreWithApiKey(1)
      let resolveStream: () => void
      const streamPromise = new Promise<void>((resolve) => {
        resolveStream = resolve
      })

      mockedSendMessageStream.mockImplementation(async () => {
        await streamPromise
      })

      const { send, loading } = useChatStream()

      const firstSend = send('first')
      expect(loading.value).toBe(true)

      // Second send should be ignored because loading is true
      await send('second')
      expect(mockedSendMessageStream).toHaveBeenCalledTimes(1)

      resolveStream!()
      await firstSend
    })
  })

  describe('abort', () => {
    it('should be a function', () => {
      setupStoreWithApiKey(1)
      const { abort } = useChatStream()
      expect(typeof abort).toBe('function')
    })
  })

  describe('messages ref', () => {
    it('should return reactive messages from the store', () => {
      setupStoreWithApiKey(1)
      const { messages } = useChatStream()
      expect(messages.value).toEqual([])
    })
  })
})
