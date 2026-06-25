import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Dashboard from '@/views/Dashboard.vue'

const mockPush = vi.fn()
const mockFetchUser = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ query: {}, params: {}, path: '/dashboard', meta: { title: '仪表盘' } }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    user: { username: 'testuser', plan: 'free' },
    fetchUser: mockFetchUser,
  }),
}))

vi.mock('@/api/knowledge', () => ({
  getKnowledgeBases: vi.fn().mockResolvedValue({ data: [{ id: 1 }, { id: 2 }] }),
}))

vi.mock('@/api/chat', () => ({
  getConversations: vi.fn().mockResolvedValue({ data: [{ id: 1 }] }),
}))

vi.mock('@/components/ui/PageHeader.vue', () => ({
  default: { name: 'PageHeader', template: '<div class="page-header" />' },
}))

vi.mock('@/components/ui/StatCard.vue', () => ({
  default: { name: 'StatCard', template: '<div class="stat-card" />' },
}))

vi.mock('@/components/ui/SectionCard.vue', () => ({
  default: { name: 'SectionCard', template: '<div class="section-card"><div class="section-title">{{ title }}</div><slot /></div>', props: ['title', 'icon'] },
}))

vi.mock('@/components/ui/ShineButton.vue', () => ({
  default: { name: 'ShineButton', template: '<button class="shine-btn"><slot /></button>' },
}))

vi.mock('@/components/ui/GlowButton.vue', () => ({
  default: { name: 'GlowButton', template: '<button class="glow-btn"><slot /></button>' },
}))

describe('Dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the dashboard page', async () => {
    const wrapper = mount(Dashboard)
    await flushPromises()
    expect(wrapper.find('.page-container').exists()).toBe(true)
  })

  it('renders stat cards', async () => {
    const wrapper = mount(Dashboard)
    await flushPromises()
    const statCards = wrapper.findAllComponents({ name: 'StatCard' })
    expect(statCards).toHaveLength(3)
  })

  it('renders quick actions section', async () => {
    const wrapper = mount(Dashboard)
    await flushPromises()
    expect(wrapper.text()).toContain('快速操作')
  })

  it('renders getting started section', async () => {
    const wrapper = mount(Dashboard)
    await flushPromises()
    expect(wrapper.text()).toContain('快速上手')
  })

  it('renders steps component', async () => {
    const wrapper = mount(Dashboard)
    await flushPromises()
    const steps = wrapper.findComponent({ name: 'ElSteps' })
    expect(steps.exists()).toBe(true)
  })

  it('fetches user data on mount', async () => {
    mount(Dashboard)
    await flushPromises()
    expect(mockFetchUser).toHaveBeenCalled()
  })

  it('renders action buttons', async () => {
    const wrapper = mount(Dashboard)
    await flushPromises()
    expect(wrapper.text()).toContain('创建知识库')
    expect(wrapper.text()).toContain('查看对话')
    expect(wrapper.text()).toContain('系统设置')
  })
})
