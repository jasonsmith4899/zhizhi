import request from './request'
import type { ApiResponse, Conversation } from '../types/api'

export function getConversations() {
  return request.get<any, ApiResponse<Conversation[]>>('/chat/conversations')
}

export function getMessages(conversationId: number) {
  return request.get<any, ApiResponse>(`/chat/conversations/${conversationId}/messages`)
}

export function deleteConversation(conversationId: number) {
  return request.delete(`/chat/conversations/${conversationId}`)
}

export function sendMessage(data: {
  message: string
  knowledgeBaseId?: number
  sessionId?: string
}) {
  return request.post<any, ApiResponse>('/chat', data)
}

export async function sendMessageStream(
  data: { message: string; knowledgeBaseId?: number; sessionId?: string },
  callbacks: {
    onChunk: (text: string) => void
    onSources: (sources: any[]) => void
    onSession: (sessionId: string) => void
    onDone: () => void
    onError: (error: string) => void
  },
  signal?: AbortSignal
) {
  const token = localStorage.getItem('token')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'

  const response = await fetch(`${baseUrl}/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(data),
    signal,
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || '请求失败')
  }

  const reader = response.body?.getReader()
  if (!reader) throw new Error('无法读取响应流')

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    let currentEvent = ''
    for (const line of lines) {
      if (line === '') {
        currentEvent = ''
        continue
      }
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        const dataStr = line.slice(5).trim()
        try {
          if (currentEvent === 'session') {
            const parsed = JSON.parse(dataStr)
            callbacks.onSession(parsed.sessionId)
          } else if (currentEvent === 'chunk') {
            callbacks.onChunk(dataStr)
          } else if (currentEvent === 'sources') {
            callbacks.onSources(JSON.parse(dataStr))
          } else if (currentEvent === 'done') {
            callbacks.onDone()
          } else if (currentEvent === 'error') {
            callbacks.onError(dataStr)
          }
        } catch (e) {
          console.warn('SSE parse error:', e, 'raw:', dataStr)
          if (currentEvent === 'sources') {
            callbacks.onSources([])
          }
        }
        currentEvent = ''
      }
    }
  }
}
