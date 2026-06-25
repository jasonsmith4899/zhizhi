import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { getConversations, getMessages, deleteConversation, sendMessage, sendMessageStream } from '@/api/chat'

vi.mock('@/api/request', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      delete: vi.fn(),
    },
  }
})

import request from '@/api/request'
const mockedRequest = vi.mocked(request)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/chat', () => {
  describe('getConversations', () => {
    it('should GET /chat/conversations', async () => {
      const res = { code: 200, data: [] }
      mockedRequest.get.mockResolvedValue(res)
      const result = await getConversations()
      expect(mockedRequest.get).toHaveBeenCalledWith('/chat/conversations')
      expect(result).toBe(res)
    })
  })

  describe('getMessages', () => {
    it('should GET /chat/conversations/:id/messages', async () => {
      const res = { code: 200, data: [] }
      mockedRequest.get.mockResolvedValue(res)
      const result = await getMessages(42)
      expect(mockedRequest.get).toHaveBeenCalledWith('/chat/conversations/42/messages')
      expect(result).toBe(res)
    })
  })

  describe('deleteConversation', () => {
    it('should DELETE /chat/conversations/:id', async () => {
      const res = { code: 200, data: null }
      mockedRequest.delete.mockResolvedValue(res)
      const result = await deleteConversation(42)
      expect(mockedRequest.delete).toHaveBeenCalledWith('/chat/conversations/42')
      expect(result).toBe(res)
    })
  })

  describe('sendMessage', () => {
    it('should POST /chat with message data', async () => {
      const res = { code: 200, data: { content: 'reply' } }
      mockedRequest.post.mockResolvedValue(res)
      const result = await sendMessage({ message: 'hello', knowledgeBaseId: 1 })
      expect(mockedRequest.post).toHaveBeenCalledWith('/chat', {
        message: 'hello',
        knowledgeBaseId: 1,
      })
      expect(result).toBe(res)
    })

    it('should pass apiKeyId and sessionId when provided', async () => {
      mockedRequest.post.mockResolvedValue({ code: 200, data: {} })
      await sendMessage({ message: 'hi', apiKeyId: 5, sessionId: 'sess-123' })
      expect(mockedRequest.post).toHaveBeenCalledWith('/chat', {
        message: 'hi',
        apiKeyId: 5,
        sessionId: 'sess-123',
      })
    })
  })

  describe('sendMessageStream', () => {
    function createMockReadableStream(chunks: string[]) {
      let index = 0
      return {
        getReader() {
          return {
            read() {
              if (index < chunks.length) {
                const value = new TextEncoder().encode(chunks[index++])
                return Promise.resolve({ value, done: false })
              }
              return Promise.resolve({ value: undefined, done: true })
            },
          }
        },
      }
    }

    const originalFetch = global.fetch

    afterEach(() => {
      global.fetch = originalFetch
    })

    it('should parse session, chunk, sources, and done events', async () => {
      const sseData = [
        'event:session\ndata:{"sessionId":"sess-abc"}\n\n',
        'event:chunk\ndata:Hello\n\n',
        'event:chunk\ndata: World\n\n',
        'event:sources\ndata:[{"documentId":1,"documentName":"doc.txt","content":"snippet","score":0.9}]\n\n',
        'event:done\ndata:\n\n',
      ]

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        body: createMockReadableStream(sseData),
      })

      const callbacks = {
        onChunk: vi.fn(),
        onSources: vi.fn(),
        onKagSources: vi.fn(),
        onSession: vi.fn(),
        onDone: vi.fn(),
        onError: vi.fn(),
      }

      await sendMessageStream(
        { message: 'hello', apiKeyId: 1, sessionId: 'old-sess' },
        callbacks,
      )

      expect(global.fetch).toHaveBeenCalledTimes(1)
      const [url, options] = (global.fetch as any).mock.calls[0]
      expect(url).toContain('/chat/stream')
      expect(options.method).toBe('POST')
      expect(options.body).toContain('"message":"hello"')

      expect(callbacks.onSession).toHaveBeenCalledWith('sess-abc')
      expect(callbacks.onChunk).toHaveBeenCalledTimes(2)
      expect(callbacks.onChunk).toHaveBeenCalledWith('Hello')
      expect(callbacks.onChunk).toHaveBeenCalledWith('World')
      expect(callbacks.onSources).toHaveBeenCalledWith([
        { documentId: 1, documentName: 'doc.txt', content: 'snippet', score: 0.9 },
      ])
      expect(callbacks.onDone).toHaveBeenCalledTimes(1)
      expect(callbacks.onError).not.toHaveBeenCalled()
    })

    it('should parse kag_sources events', async () => {
      const sseData = [
        'event:kag_sources\ndata:[{"name":"entity1"}]\n\n',
        'event:done\ndata:\n\n',
      ]

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        body: createMockReadableStream(sseData),
      })

      const callbacks = {
        onChunk: vi.fn(),
        onSources: vi.fn(),
        onKagSources: vi.fn(),
        onSession: vi.fn(),
        onDone: vi.fn(),
        onError: vi.fn(),
      }

      await sendMessageStream({ message: 'test' }, callbacks)

      expect(callbacks.onKagSources).toHaveBeenCalledWith([{ name: 'entity1' }])
    })

    it('should call onError on error event', async () => {
      const sseData = ['event:error\ndata:Something went wrong\n\n']

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        body: createMockReadableStream(sseData),
      })

      const callbacks = {
        onChunk: vi.fn(),
        onSources: vi.fn(),
        onKagSources: vi.fn(),
        onSession: vi.fn(),
        onDone: vi.fn(),
        onError: vi.fn(),
      }

      await sendMessageStream({ message: 'test' }, callbacks)

      expect(callbacks.onError).toHaveBeenCalledWith('Something went wrong')
    })

    it('should throw when response is not ok', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        text: () => Promise.resolve('Internal Server Error'),
      })

      const callbacks = {
        onChunk: vi.fn(),
        onSources: vi.fn(),
        onKagSources: vi.fn(),
        onSession: vi.fn(),
        onDone: vi.fn(),
        onError: vi.fn(),
      }

      await expect(sendMessageStream({ message: 'test' }, callbacks)).rejects.toThrow(
        'Internal Server Error',
      )
    })

    it('should throw when body is null', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        body: null,
      })

      const callbacks = {
        onChunk: vi.fn(),
        onSources: vi.fn(),
        onKagSources: vi.fn(),
        onSession: vi.fn(),
        onDone: vi.fn(),
        onError: vi.fn(),
      }

      await expect(sendMessageStream({ message: 'test' }, callbacks)).rejects.toThrow(
        '无法读取响应流',
      )
    })

    it('should include Authorization header when token exists', async () => {
      localStorage.setItem('token', 'my-token')

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        body: createMockReadableStream([]),
      })

      const callbacks = {
        onChunk: vi.fn(),
        onSources: vi.fn(),
        onKagSources: vi.fn(),
        onSession: vi.fn(),
        onDone: vi.fn(),
        onError: vi.fn(),
      }

      await sendMessageStream({ message: 'test' }, callbacks)

      const [, options] = (global.fetch as any).mock.calls[0]
      expect(options.headers.Authorization).toBe('Bearer my-token')

      localStorage.removeItem('token')
    })

    it('should pass AbortSignal to fetch', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        body: createMockReadableStream([]),
      })

      const controller = new AbortController()
      const callbacks = {
        onChunk: vi.fn(),
        onSources: vi.fn(),
        onKagSources: vi.fn(),
        onSession: vi.fn(),
        onDone: vi.fn(),
        onError: vi.fn(),
      }

      await sendMessageStream({ message: 'test' }, callbacks, controller.signal)

      const [, options] = (global.fetch as any).mock.calls[0]
      expect(options.signal).toBe(controller.signal)
    })

    it('should handle sources parse error gracefully', async () => {
      const sseData = [
        'event:sources\ndata:not-valid-json\n\n',
        'event:done\ndata:\n\n',
      ]

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        body: createMockReadableStream(sseData),
      })

      const callbacks = {
        onChunk: vi.fn(),
        onSources: vi.fn(),
        onKagSources: vi.fn(),
        onSession: vi.fn(),
        onDone: vi.fn(),
        onError: vi.fn(),
      }

      // Should not throw; sources callback receives [] on parse error
      await sendMessageStream({ message: 'test' }, callbacks)
      expect(callbacks.onSources).toHaveBeenCalledWith([])
      expect(callbacks.onDone).toHaveBeenCalled()
    })
  })
})
