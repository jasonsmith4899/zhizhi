export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface LoginData {
  token: string
  refreshToken: string
  user: User
}

export interface User {
  id: number
  username: string
  email: string
  plan: string
}

export interface KnowledgeBase {
  id: number
  name: string
  description: string
  documentCount: number
  createdAt: string
  updatedAt: string
}

export interface Document {
  id: number
  name: string
  status: string
  knowledgeBaseId: number
  createdAt: string
}

export interface Conversation {
  id: number
  title: string
  knowledgeBaseId: number
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  sources?: Source[]
  time: Date
}

export interface Source {
  documentId: number
  documentName: string
  content: string
  score: number
}

export interface DocumentPreview {
  id: number
  name: string
  content: string
  chunks: { content: string; index: number }[]
}
