import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import KgOverview from '@/components/kg/KgOverview.vue'

const mockGetKgStats = vi.fn()

vi.mock('@/api/kg', () => ({
  getKgStats: (...args: any[]) => mockGetKgStats(...args),
  getKgEntities: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
  getKgEntityDetail: vi.fn().mockResolvedValue({ data: null }),
  getKgRelations: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
  getKgGraph: vi.fn().mockResolvedValue({ data: { nodes: [], edges: [] } }),
  searchKgEntities: vi.fn().mockResolvedValue({ data: [] }),
}))

vi.mock('@/components/ui/SectionCard.vue', () => ({
  default: { name: 'SectionCard', template: '<div class="section-card"><slot name="header-actions" /><slot /></div>', props: ['title', 'icon'] },
}))

vi.mock('@/components/ui/StatCard.vue', () => ({
  default: { name: 'StatCard', template: '<div class="stat-card" />' },
}))

vi.mock('@/components/ui/GlassCard.vue', () => ({
  default: { name: 'GlassCard', template: '<div class="glass-card"><slot /></div>' },
}))

vi.mock('@/components/ui/EmptyState.vue', () => ({
  default: { name: 'EmptyState', template: '<div class="empty-state" />' },
}))

vi.mock('@/components/ui/ShineButton.vue', () => ({
  default: { name: 'ShineButton', template: '<button class="shine-btn"><slot /></button>' },
}))

vi.mock('@/components/ui/ScoreTag.vue', () => ({
  default: { name: 'ScoreTag', template: '<span class="score-tag" />' },
}))

vi.mock('vis-network/standalone', () => ({
  Network: vi.fn().mockImplementation(() => ({
    on: vi.fn(),
    destroy: vi.fn(),
    focus: vi.fn(),
    selectNodes: vi.fn(),
  })),
  DataSet: vi.fn().mockImplementation(() => ({
    getIds: vi.fn().mockReturnValue([]),
    update: vi.fn(),
  })),
}))

describe('KgOverview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders stat cards', async () => {
    mockGetKgStats.mockResolvedValue({
      data: {
        entityCount: 100,
        relationCount: 50,
        avgMentionCount: 5.5,
        topEntities: [],
        typeDistribution: [],
      },
    })

    const wrapper = mount(KgOverview, { props: { kbId: 1 } })
    await flushPromises()
    const statCards = wrapper.findAllComponents({ name: 'StatCard' })
    expect(statCards).toHaveLength(3)
  })

  it('shows empty state when entity count is 0', async () => {
    mockGetKgStats.mockResolvedValue({
      data: {
        entityCount: 0,
        relationCount: 0,
        avgMentionCount: 0,
        topEntities: [],
        typeDistribution: [],
      },
    })

    const wrapper = mount(KgOverview, { props: { kbId: 1 } })
    await flushPromises()
    expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(true)
  })

  it('shows data sections when entities exist', async () => {
    mockGetKgStats.mockResolvedValue({
      data: {
        entityCount: 10,
        relationCount: 5,
        avgMentionCount: 3,
        topEntities: [
          { name: 'Entity A', type: '人物', mentionCount: 10 },
        ],
        typeDistribution: [
          { type: '人物', count: 5 },
          { type: '组织', count: 3 },
        ],
      },
    })

    const wrapper = mount(KgOverview, { props: { kbId: 1 } })
    await flushPromises()
    expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(false)
    expect(wrapper.findAllComponents({ name: 'SectionCard' }).length).toBeGreaterThanOrEqual(2)
  })

  it('loads stats on mount', async () => {
    mockGetKgStats.mockResolvedValue({
      data: { entityCount: 0, relationCount: 0, avgMentionCount: 0, topEntities: [], typeDistribution: [] },
    })
    mount(KgOverview, { props: { kbId: 1 } })
    await flushPromises()
    expect(mockGetKgStats).toHaveBeenCalledWith(1)
  })

  it('renders graph visualization button when data exists', async () => {
    mockGetKgStats.mockResolvedValue({
      data: {
        entityCount: 10,
        relationCount: 5,
        avgMentionCount: 3,
        topEntities: [],
        typeDistribution: [],
      },
    })

    const wrapper = mount(KgOverview, { props: { kbId: 1 } })
    await flushPromises()
    expect(wrapper.find('.shine-btn').exists()).toBe(true)
    expect(wrapper.find('.shine-btn').text()).toContain('图谱可视化')
  })

  it('renders sub-tabs for entity and relation management', async () => {
    mockGetKgStats.mockResolvedValue({
      data: {
        entityCount: 10,
        relationCount: 5,
        avgMentionCount: 3,
        topEntities: [],
        typeDistribution: [],
      },
    })

    const wrapper = mount(KgOverview, { props: { kbId: 1 } })
    await flushPromises()
    const tabs = wrapper.findComponent({ name: 'ElTabs' })
    expect(tabs.exists()).toBe(true)
  })

  it('reloads stats when kbId changes', async () => {
    mockGetKgStats.mockResolvedValue({
      data: { entityCount: 0, relationCount: 0, avgMentionCount: 0, topEntities: [], typeDistribution: [] },
    })

    const wrapper = mount(KgOverview, { props: { kbId: 1 } })
    await flushPromises()
    expect(mockGetKgStats).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ kbId: 2 })
    await flushPromises()
    expect(mockGetKgStats).toHaveBeenCalledTimes(2)
  })
})
