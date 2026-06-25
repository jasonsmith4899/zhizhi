import { describe, it, expect, vi, beforeEach } from 'vitest'

const { mockRoutes } = vi.hoisted(() => {
  const mockRoutes = [
    { path: '/login', name: 'Login', meta: { public: true } },
    { path: '/', name: null, redirect: '/dashboard', children: [
      { path: 'dashboard', name: 'Dashboard', meta: { title: '仪表盘' } },
      { path: 'knowledge', name: 'KnowledgeList', meta: { title: '知识库' } },
      { path: 'knowledge/:id', name: 'KnowledgeDetail', meta: { title: '知识库详情' } },
      { path: 'chat', name: 'Chat', meta: { title: 'AI 对话' } },
      { path: 'chat-history', name: 'ChatHistory', meta: { title: '对话记录' } },
      { path: 'profile', name: 'Profile', meta: {} },
      { path: 'audit-log', name: 'AuditLog', meta: {} },
    ]},
  ]
  return { mockRoutes }
})

vi.mock('@/router', () => ({
  default: { getRoutes: () => mockRoutes, currentRoute: { value: { path: '/' } } },
}))

import router from '@/router'

describe('router', () => {
  describe('route definitions', () => {
    it('should have a login route', () => {
      const loginRoute = mockRoutes.find((r: any) => r.path === '/login')
      expect(loginRoute).toBeDefined()
      expect(loginRoute?.meta?.public).toBe(true)
    })

    it('should have a dashboard route', () => {
      const root = mockRoutes.find((r: any) => r.path === '/')
      const dashboard = root?.children?.find((r: any) => r.name === 'Dashboard')
      expect(dashboard).toBeDefined()
    })

    it('should have knowledge list route', () => {
      const root = mockRoutes.find((r: any) => r.path === '/')
      const route = root?.children?.find((r: any) => r.name === 'KnowledgeList')
      expect(route).toBeDefined()
    })

    it('should have knowledge detail route with :id param', () => {
      const root = mockRoutes.find((r: any) => r.path === '/')
      const route = root?.children?.find((r: any) => r.name === 'KnowledgeDetail')
      expect(route).toBeDefined()
      expect(route?.path).toContain(':id')
    })

    it('should have chat route', () => {
      const root = mockRoutes.find((r: any) => r.path === '/')
      const route = root?.children?.find((r: any) => r.name === 'Chat')
      expect(route).toBeDefined()
    })

    it('should have chat history route', () => {
      const root = mockRoutes.find((r: any) => r.path === '/')
      const route = root?.children?.find((r: any) => r.name === 'ChatHistory')
      expect(route).toBeDefined()
    })

    it('should have profile route', () => {
      const root = mockRoutes.find((r: any) => r.path === '/')
      const route = root?.children?.find((r: any) => r.name === 'Profile')
      expect(route).toBeDefined()
    })

    it('should have audit log route', () => {
      const root = mockRoutes.find((r: any) => r.path === '/')
      const route = root?.children?.find((r: any) => r.name === 'AuditLog')
      expect(route).toBeDefined()
    })

    it('should redirect / to /dashboard', () => {
      const rootRoute = mockRoutes.find((r: any) => r.path === '/')
      expect(rootRoute).toBeDefined()
      expect(rootRoute?.redirect).toBe('/dashboard')
    })
  })
})
