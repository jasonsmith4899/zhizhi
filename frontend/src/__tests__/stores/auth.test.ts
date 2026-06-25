import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  getCurrentUser: vi.fn(),
}))

import { useAuthStore } from '@/stores/auth'
import { login, register, getCurrentUser } from '@/api/auth'

const mockedLogin = vi.mocked(login)
const mockedRegister = vi.mocked(register)
const mockedGetCurrentUser = vi.mocked(getCurrentUser)

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  localStorage.clear()
})

describe('stores/auth', () => {
  describe('initial state', () => {
    it('should have empty token and null user by default', () => {
      const store = useAuthStore()
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(store.isLoggedIn).toBe(false)
    })

    it('should restore token from localStorage', () => {
      localStorage.setItem('token', 'saved-token')
      setActivePinia(createPinia())
      const store = useAuthStore()
      expect(store.token).toBe('saved-token')
      expect(store.isLoggedIn).toBe(true)
    })
  })

  describe('setToken', () => {
    it('should update token and persist to localStorage', () => {
      const store = useAuthStore()
      store.setToken('new-token')
      expect(store.token).toBe('new-token')
      expect(localStorage.getItem('token')).toBe('new-token')
    })
  })

  describe('login', () => {
    it('should call API login and store token/user', async () => {
      const loginResponse = {
        data: {
          token: 'jwt-token',
          refreshToken: 'refresh-token',
          user: { id: 1, username: 'test', email: 'test@example.com', plan: 'free' },
        },
      }
      mockedLogin.mockResolvedValue(loginResponse as any)

      const store = useAuthStore()
      await store.login('test', 'password')

      expect(mockedLogin).toHaveBeenCalledWith({ username: 'test', password: 'password' })
      expect(store.token).toBe('jwt-token')
      expect(store.user).toEqual(loginResponse.data.user)
      expect(localStorage.getItem('token')).toBe('jwt-token')
      expect(localStorage.getItem('refreshToken')).toBe('refresh-token')
    })

    it('should propagate API errors', async () => {
      mockedLogin.mockRejectedValue(new Error('Invalid credentials'))
      const store = useAuthStore()
      await expect(store.login('bad', 'bad')).rejects.toThrow('Invalid credentials')
    })
  })

  describe('register', () => {
    it('should call API register and store token/user', async () => {
      const registerResponse = {
        data: {
          token: 'new-jwt',
          refreshToken: 'new-refresh',
          user: { id: 2, username: 'newuser', email: 'new@example.com', plan: 'free' },
        },
      }
      mockedRegister.mockResolvedValue(registerResponse as any)

      const store = useAuthStore()
      await store.register('newuser', 'new@example.com', 'password')

      expect(mockedRegister).toHaveBeenCalledWith({
        username: 'newuser',
        email: 'new@example.com',
        password: 'password',
      })
      expect(store.token).toBe('new-jwt')
      expect(store.user).toEqual(registerResponse.data.user)
      expect(localStorage.getItem('token')).toBe('new-jwt')
      expect(localStorage.getItem('refreshToken')).toBe('new-refresh')
    })
  })

  describe('fetchUser', () => {
    it('should fetch current user and set it', async () => {
      const user = { id: 1, username: 'test', email: 'test@example.com', plan: 'free' }
      mockedGetCurrentUser.mockResolvedValue({ code: 200, data: user } as any)

      const store = useAuthStore()
      store.setToken('valid-token')
      await store.fetchUser()

      expect(mockedGetCurrentUser).toHaveBeenCalled()
      expect(store.user).toEqual(user)
    })

    it('should not call API when token is empty', async () => {
      const store = useAuthStore()
      await store.fetchUser()
      expect(mockedGetCurrentUser).not.toHaveBeenCalled()
    })

    it('should logout on fetch error', async () => {
      mockedGetCurrentUser.mockRejectedValue(new Error('Unauthorized'))

      const store = useAuthStore()
      store.setToken('expired-token')
      await store.fetchUser()

      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(localStorage.getItem('token')).toBeNull()
    })
  })

  describe('logout', () => {
    it('should clear token, user, and localStorage', () => {
      const store = useAuthStore()
      store.setToken('some-token')
      localStorage.setItem('refreshToken', 'some-refresh')

      store.logout()

      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(store.isLoggedIn).toBe(false)
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
    })
  })
})
