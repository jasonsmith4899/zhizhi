import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../stores/auth'

// Mock API模块
vi.mock('../api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  getCurrentUser: vi.fn(),
}))

import { login as apiLogin, register as apiRegister, getCurrentUser } from '../api/auth'

describe('AuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('默认未登录', () => {
      const store = useAuthStore()
      expect(store.isLoggedIn).toBe(false)
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
    })

    it('从localStorage恢复token', () => {
      localStorage.setItem('token', 'saved-token')
      const store = useAuthStore()
      expect(store.token).toBe('saved-token')
    })
  })

  describe('login', () => {
    it('登录成功', async () => {
      const mockResponse = {
        code: 200,
        data: {
          token: 'jwt-token-123',
          user: { id: 1, username: 'test', email: 'test@example.com', plan: 'free', apiKey: 'key-123' },
        },
      }
      vi.mocked(apiLogin).mockResolvedValue(mockResponse as any)

      const store = useAuthStore()
      await store.login('test', '123456')

      expect(store.token).toBe('jwt-token-123')
      expect(store.user?.username).toBe('test')
      expect(store.isLoggedIn).toBe(true)
      expect(localStorage.getItem('token')).toBe('jwt-token-123')
    })

    it('登录失败抛出异常', async () => {
      vi.mocked(apiLogin).mockRejectedValue(new Error('登录失败'))

      const store = useAuthStore()
      await expect(store.login('test', 'wrong')).rejects.toThrow()
      expect(store.isLoggedIn).toBe(false)
    })
  })

  describe('register', () => {
    it('注册成功', async () => {
      const mockResponse = {
        code: 200,
        data: {
          token: 'new-token',
          user: { id: 2, username: 'newuser', email: 'new@example.com', plan: 'free', apiKey: 'key-456' },
        },
      }
      vi.mocked(apiRegister).mockResolvedValue(mockResponse as any)

      const store = useAuthStore()
      await store.register('newuser', 'new@example.com', '123456')

      expect(store.token).toBe('new-token')
      expect(store.user?.username).toBe('newuser')
    })
  })

  describe('logout', () => {
    it('清除状态和localStorage', async () => {
      localStorage.setItem('token', 'some-token')
      const store = useAuthStore()

      store.logout()

      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(localStorage.getItem('token')).toBeNull()
    })
  })

  describe('fetchUser', () => {
    it('获取用户信息成功', async () => {
      localStorage.setItem('token', 'valid-token')
      vi.mocked(getCurrentUser).mockResolvedValue({
        code: 200,
        data: { id: 1, username: 'test', email: 'test@example.com', plan: 'free', apiKey: 'key' },
      } as any)

      const store = useAuthStore()
      await store.fetchUser()

      expect(store.user?.username).toBe('test')
    })

    it('无token时不请求', async () => {
      const store = useAuthStore()
      await store.fetchUser()

      expect(getCurrentUser).not.toHaveBeenCalled()
    })

    it('请求失败时logout', async () => {
      localStorage.setItem('token', 'expired-token')
      vi.mocked(getCurrentUser).mockRejectedValue(new Error('401'))

      const store = useAuthStore()
      await store.fetchUser()

      expect(store.token).toBe('')
      expect(store.user).toBeNull()
    })
  })
})
