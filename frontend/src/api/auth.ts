import request from './request'

export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

export function register(data: { username: string; email: string; password: string }) {
  return request.post('/auth/register', data)
}

export function getCurrentUser() {
  return request.get('/auth/me')
}

export function generateApiKey() {
  return request.post('/auth/api-key')
}

export function revokeApiKey() {
  return request.delete('/auth/api-key')
}

export function getApiKey() {
  return request.get('/auth/api-key')
}
