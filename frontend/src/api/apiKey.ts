import request from './request'

// API Key 管理（新接口）
export function listApiKeys() {
  return request.get('/api-keys')
}

export function createApiKey(data: {
  name?: string
  description?: string
  assistantPersona?: string
  merchantBackground?: string
  answerRules?: string
  knowledgeBaseIds?: number[]
}) {
  return request.post('/api-keys', data)
}

export function updateApiKey(id: number, data: {
  name?: string
  description?: string
  assistantPersona?: string
  merchantBackground?: string
  answerRules?: string
  knowledgeBaseIds?: number[]
}) {
  return request.put(`/api-keys/${id}`, data)
}

export function deleteApiKey(id: number) {
  return request.delete(`/api-keys/${id}`)
}
