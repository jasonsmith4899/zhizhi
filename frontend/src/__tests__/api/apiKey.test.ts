import { describe, it, expect, vi, beforeEach } from 'vitest'
import { listApiKeys, createApiKey, updateApiKey, deleteApiKey } from '@/api/apiKey'

vi.mock('@/api/request', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    },
  }
})

import request from '@/api/request'
const mockedRequest = vi.mocked(request)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/apiKey', () => {
  it('listApiKeys should GET /api-keys', async () => {
    const res = { code: 200, data: [] }
    mockedRequest.get.mockResolvedValue(res)
    const result = await listApiKeys()
    expect(mockedRequest.get).toHaveBeenCalledWith('/api-keys')
    expect(result).toBe(res)
  })

  it('createApiKey should POST /api-keys with data', async () => {
    const data = {
      name: 'my-key',
      description: 'test',
      assistantPersona: 'helper',
      merchantBackground: 'bg',
      answerRules: 'rules',
      knowledgeBaseIds: [1, 2],
    }
    mockedRequest.post.mockResolvedValue({ code: 200, data: { id: 1 } })
    const result = await createApiKey(data)
    expect(mockedRequest.post).toHaveBeenCalledWith('/api-keys', data)
    expect(result).toEqual({ code: 200, data: { id: 1 } })
  })

  it('createApiKey should work with minimal data', async () => {
    mockedRequest.post.mockResolvedValue({ code: 200, data: { id: 2 } })
    await createApiKey({})
    expect(mockedRequest.post).toHaveBeenCalledWith('/api-keys', {})
  })

  it('updateApiKey should PUT /api-keys/:id with data', async () => {
    const data = { name: 'updated-key', knowledgeBaseIds: [3] }
    mockedRequest.put.mockResolvedValue({ code: 200, data: null })
    const result = await updateApiKey(5, data)
    expect(mockedRequest.put).toHaveBeenCalledWith('/api-keys/5', data)
    expect(result).toEqual({ code: 200, data: null })
  })

  it('deleteApiKey should DELETE /api-keys/:id', async () => {
    mockedRequest.delete.mockResolvedValue({ code: 200, data: null })
    const result = await deleteApiKey(5)
    expect(mockedRequest.delete).toHaveBeenCalledWith('/api-keys/5')
    expect(result).toEqual({ code: 200, data: null })
  })
})
