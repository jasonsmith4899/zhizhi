import { describe, it, expect, vi, beforeEach } from 'vitest'

// Use vi.hoisted to create mocks that are available when vi.mock factories run
const { mockPush, mockElMessageError, mockSetToken, interceptorCallbacks } = vi.hoisted(() => {
  const interceptorCallbacks = {
    request: [] as Array<(config: any) => any>,
    responseSuccess: [] as Array<(response: any) => any>,
    responseError: [] as Array<(error: any) => any>,
  }

  return {
    mockPush: vi.fn(),
    mockElMessageError: vi.fn(),
    mockSetToken: vi.fn(),
    interceptorCallbacks,
  }
})

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mockElMessageError,
    success: vi.fn(),
  },
}))

vi.mock('@/router', () => ({
  default: { push: mockPush },
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => ({
    setToken: mockSetToken,
  })),
}))

vi.mock('axios', () => {
  const createInstance = () => ({
    interceptors: {
      request: {
        use: vi.fn((cb: (config: any) => any) => {
          interceptorCallbacks.request.push(cb)
        }),
      },
      response: {
        use: vi.fn(
          (success: (response: any) => any, error: (error: any) => any) => {
            interceptorCallbacks.responseSuccess.push(success)
            interceptorCallbacks.responseError.push(error)
          },
        ),
      },
    },
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  })

  return {
    default: { create: vi.fn(() => createInstance()) },
  }
})

// Import request.ts — this triggers interceptor registration on the mocked axios
import '@/api/request'

function getRequestInterceptor() {
  return interceptorCallbacks.request[0]
}

function getResponseSuccessInterceptor() {
  return interceptorCallbacks.responseSuccess[0]
}

function getResponseErrorInterceptor() {
  return interceptorCallbacks.responseError[0]
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})

describe('api/request', () => {
  it('should register request and response interceptors', () => {
    expect(interceptorCallbacks.request.length).toBeGreaterThanOrEqual(1)
    expect(interceptorCallbacks.responseSuccess.length).toBeGreaterThanOrEqual(1)
    expect(interceptorCallbacks.responseError.length).toBeGreaterThanOrEqual(1)
  })

  describe('request interceptor', () => {
    it('should add Authorization header when token exists', () => {
      localStorage.setItem('token', 'my-jwt-token')
      const config = { headers: {} as Record<string, string> }
      const result = getRequestInterceptor()(config)
      expect(result.headers.Authorization).toBe('Bearer my-jwt-token')
    })

    it('should not add Authorization header when no token', () => {
      const config = { headers: {} as Record<string, string> }
      const result = getRequestInterceptor()(config)
      expect(result.headers.Authorization).toBeUndefined()
    })

    it('should return the config object', () => {
      const config = { headers: {}, url: '/test' }
      const result = getRequestInterceptor()(config)
      expect(result).toBe(config)
    })
  })

  describe('response success interceptor', () => {
    it('should return data directly for blob responses', () => {
      const blobData = new Blob(['test'])
      const response = {
        config: { responseType: 'blob' },
        data: blobData,
      }
      const result = getResponseSuccessInterceptor()(response)
      expect(result).toBe(blobData)
    })

    it('should return full response data when code is 200', () => {
      const response = {
        config: {},
        data: { code: 200, data: { id: 1 }, message: 'ok' },
      }
      const result = getResponseSuccessInterceptor()(response)
      expect(result).toEqual({ code: 200, data: { id: 1 }, message: 'ok' })
    })

    it('should show error message and reject when code is not 200', async () => {
      const response = {
        config: {},
        data: { code: 500, message: 'Server Error', data: null },
      }
      await expect(getResponseSuccessInterceptor()(response)).rejects.toEqual({
        code: 500,
        message: 'Server Error',
        data: null,
      })
      expect(mockElMessageError).toHaveBeenCalledWith('Server Error')
    })

    it('should show default error message when message is empty', async () => {
      const response = {
        config: {},
        data: { code: 500, message: '', data: null },
      }
      await expect(getResponseSuccessInterceptor()(response)).rejects.toBeDefined()
      expect(mockElMessageError).toHaveBeenCalledWith('请求失败')
    })

    it('should clear tokens and redirect to /login on 401 code', async () => {
      localStorage.setItem('token', 'old-token')
      localStorage.setItem('refreshToken', 'old-refresh')
      const response = {
        config: {},
        data: { code: 401, message: 'Unauthorized', data: null },
      }
      await expect(getResponseSuccessInterceptor()(response)).rejects.toBeDefined()
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
      expect(mockPush).toHaveBeenCalledWith('/login')
    })
  })

  describe('response error interceptor', () => {
    it('should redirect to /login when 401 and no refresh token', async () => {
      const error = {
        config: {},
        response: { status: 401 },
      }
      await expect(getResponseErrorInterceptor()(error)).rejects.toBe(error)
      expect(mockPush).toHaveBeenCalledWith('/login')
    })

    it('should show network error message for non-401 errors', async () => {
      const error = {
        config: {},
        response: { status: 500, data: { message: 'Internal Error' } },
      }
      await expect(getResponseErrorInterceptor()(error)).rejects.toBe(error)
      expect(mockElMessageError).toHaveBeenCalledWith('Internal Error')
    })

    it('should show generic network error when no response message', async () => {
      const error = {
        config: {},
        response: { status: 500, data: {} },
      }
      await expect(getResponseErrorInterceptor()(error)).rejects.toBe(error)
      expect(mockElMessageError).toHaveBeenCalledWith('网络错误')
    })

    it('should show network error when no response object', async () => {
      const error = {
        config: {},
        response: undefined,
      }
      await expect(getResponseErrorInterceptor()(error)).rejects.toBe(error)
      expect(mockElMessageError).toHaveBeenCalledWith('网络错误')
    })

    it('should mark request as retried when attempting refresh', async () => {
      localStorage.setItem('refreshToken', 'valid-refresh-token')

      const originalRequest = {
        headers: {} as Record<string, string>,
        url: '/test',
        _retry: false,
      }
      const error = {
        config: originalRequest,
        response: { status: 401 },
      }

      // The refreshRequest.post call — we need to mock the post on the second
      // axios.create instance. Both instances are the same mock object since
      // the factory returns a new instance each time. The first instance is
      // the main request (exported as `request`), the second is refreshRequest.
      // Since vi.clearAllMocks runs in beforeEach, we set up the mock here.
      // refreshRequest.post is called as: refreshRequest.post('/auth/refresh', { refreshToken })
      // We need to get a reference to the refreshRequest mock instance.
      // With vi.mock hoisting and vi.hoisted, we can capture it.

      // Since both instances are created by the factory and we don't have
      // a reference to the second one, let's test what we can:
      // The function should mark the request as _retry = true
      try {
        await getResponseErrorInterceptor()(error)
      } catch {
        // May reject depending on refresh flow
      }

      expect(originalRequest._retry).toBe(true)
    })
  })
})
