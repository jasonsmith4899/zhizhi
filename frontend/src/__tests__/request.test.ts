import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock axios
vi.mock('axios', () => {
  const interceptors = {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  }
  return {
    default: {
      create: vi.fn(() => ({
        interceptors,
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
      })),
    },
  }
})

// Mock router
vi.mock('../router', () => ({
  default: { push: vi.fn() },
}))

describe('API请求模块', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('创建axios实例时设置baseURL', async () => {
    const axios = await import('axios')
    await import('../api/request')

    expect(axios.default.create).toHaveBeenCalledWith(
      expect.objectContaining({
        baseURL: '/api/v1',
        timeout: 30000,
      })
    )
  })
})
