import { describe, it, expect, vi, beforeEach } from 'vitest'
import { login, register, getCurrentUser } from '@/api/auth'

vi.mock('@/api/request', () => {
  return {
    default: {
      post: vi.fn(),
      get: vi.fn(),
    },
  }
})

import request from '@/api/request'

const mockedRequest = vi.mocked(request)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/auth', () => {
  describe('login', () => {
    it('should POST to /auth/login with credentials', async () => {
      const responseData = {
        code: 200,
        data: { token: 'abc', refreshToken: 'def', user: { id: 1, username: 'test' } },
      }
      mockedRequest.post.mockResolvedValue(responseData)

      const result = await login({ username: 'test', password: 'pass' })

      expect(mockedRequest.post).toHaveBeenCalledWith('/auth/login', {
        username: 'test',
        password: 'pass',
      })
      expect(result).toBe(responseData)
    })

    it('should propagate errors', async () => {
      mockedRequest.post.mockRejectedValue(new Error('Unauthorized'))

      await expect(login({ username: 'bad', password: 'bad' })).rejects.toThrow('Unauthorized')
    })
  })

  describe('register', () => {
    it('should POST to /auth/register with user data', async () => {
      const responseData = {
        code: 200,
        data: { token: 'abc', refreshToken: 'def', user: { id: 1, username: 'newuser' } },
      }
      mockedRequest.post.mockResolvedValue(responseData)

      const result = await register({ username: 'newuser', email: 'a@b.com', password: 'pass' })

      expect(mockedRequest.post).toHaveBeenCalledWith('/auth/register', {
        username: 'newuser',
        email: 'a@b.com',
        password: 'pass',
      })
      expect(result).toBe(responseData)
    })
  })

  describe('getCurrentUser', () => {
    it('should GET /auth/me', async () => {
      const responseData = {
        code: 200,
        data: { id: 1, username: 'test', email: 'a@b.com', plan: 'free' },
      }
      mockedRequest.get.mockResolvedValue(responseData)

      const result = await getCurrentUser()

      expect(mockedRequest.get).toHaveBeenCalledWith('/auth/me')
      expect(result).toBe(responseData)
    })
  })
})
