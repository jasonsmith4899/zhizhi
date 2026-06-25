import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { nextTick } from 'vue'

vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    error: vi.fn(),
  },
}))

import { useChatStore } from '@/stores/chat'

const STORAGE_KEY = 'zhizhi-chat-state'

beforeEach(() => {
  setActivePinia(createPinia())
  localStorage.clear()
  vi.clearAllMocks()
})

describe('stores/chat', () => {
  describe('initial state', () => {
    it('should have empty messages and undefined sessionId by default', () => {
      const store = useChatStore()
      expect(store.messages).toEqual([])
      expect(store.sessionId).toBeUndefined()
      expect(store.selectedApiKeyId).toBeUndefined()
    })

    it('should restore state from localStorage', () => {
      const savedState = {
        messages: [
          { role: 'user', content: 'hello', time: '2024-01-01T00:00:00.000Z' },
          { role: 'assistant', content: 'hi', time: '2024-01-01T00:00:01.000Z' },
        ],
        sessionId: 'sess-123',
        selectedApiKeyId: 5,
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(savedState))
      setActivePinia(createPinia())

      const store = useChatStore()
      expect(store.messages).toHaveLength(2)
      expect(store.messages[0].role).toBe('user')
      expect(store.messages[0].content).toBe('hello')
      expect(store.messages[0].time).toBeInstanceOf(Date)
      expect(store.sessionId).toBe('sess-123')
      expect(store.selectedApiKeyId).toBe(5)
    })

    it('should handle legacy selectedKbId in localStorage', () => {
      const savedState = {
        messages: [],
        sessionId: undefined,
        selectedKbId: 3,
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(savedState))
      setActivePinia(createPinia())

      const store = useChatStore()
      expect(store.selectedApiKeyId).toBe(3)
    })

    it('should handle corrupt localStorage data gracefully', () => {
      localStorage.setItem(STORAGE_KEY, 'not-valid-json')
      setActivePinia(createPinia())

      const store = useChatStore()
      expect(store.messages).toEqual([])
      expect(store.sessionId).toBeUndefined()
    })
  })

  describe('newChat', () => {
    it('should clear messages and sessionId', () => {
      const store = useChatStore()
      store.messages.push(
        { role: 'user', content: 'test', time: new Date() },
        { role: 'assistant', content: 'reply', time: new Date() },
      )
      store.sessionId = 'some-session'

      store.newChat()

      expect(store.messages).toEqual([])
      expect(store.sessionId).toBeUndefined()
    })
  })

  describe('persistence', () => {
    it('should save state to localStorage when messages change', async () => {
      const store = useChatStore()
      store.messages.push({ role: 'user', content: 'hello', time: new Date() })

      // Wait for the watcher to fire
      await nextTick()
      await nextTick()

      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
      expect(saved.messages).toHaveLength(1)
      expect(saved.messages[0].content).toBe('hello')
    })

    it('should save sessionId to localStorage', async () => {
      const store = useChatStore()
      store.sessionId = 'new-session'

      await nextTick()
      await nextTick()

      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
      expect(saved.sessionId).toBe('new-session')
    })

    it('should limit persisted messages to MAX_PERSISTED_MESSAGES (50)', async () => {
      const store = useChatStore()
      // Add 60 messages
      for (let i = 0; i < 60; i++) {
        store.messages.push({ role: 'user', content: `msg-${i}`, time: new Date() })
      }

      await nextTick()
      await nextTick()

      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
      // Should only persist the last 50
      expect(saved.messages).toHaveLength(50)
      expect(saved.messages[0].content).toBe('msg-10')
      expect(saved.messages[49].content).toBe('msg-59')
    })
  })

  describe('cross-tab sync', () => {
    it('should update state when storage event fires', async () => {
      const store = useChatStore()

      const newState = {
        messages: [
          { role: 'user', content: 'from other tab', time: '2024-01-01T00:00:00.000Z' },
        ],
        sessionId: 'other-session',
        selectedApiKeyId: 99,
      }

      // Simulate a storage event
      const event = new StorageEvent('storage', {
        key: STORAGE_KEY,
        newValue: JSON.stringify(newState),
      })
      window.dispatchEvent(event)

      await nextTick()

      expect(store.messages).toHaveLength(1)
      expect(store.messages[0].content).toBe('from other tab')
      expect(store.messages[0].time).toBeInstanceOf(Date)
      expect(store.sessionId).toBe('other-session')
      expect(store.selectedApiKeyId).toBe(99)
    })

    it('should ignore storage events for other keys', () => {
      const store = useChatStore()
      const originalMessages = [...store.messages]

      const event = new StorageEvent('storage', {
        key: 'some-other-key',
        newValue: '{"something": true}',
      })
      window.dispatchEvent(event)

      expect(store.messages).toEqual(originalMessages)
    })

    it('should ignore storage events with null newValue', () => {
      const store = useChatStore()
      const originalMessages = [...store.messages]

      const event = new StorageEvent('storage', {
        key: STORAGE_KEY,
        newValue: null,
      })
      window.dispatchEvent(event)

      expect(store.messages).toEqual(originalMessages)
    })
  })
})
