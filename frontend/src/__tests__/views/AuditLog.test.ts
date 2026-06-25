import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AuditLog from '@/views/settings/AuditLog.vue'

const mockGetAuditLogs = vi.fn()

vi.mock('@/api/audit', () => ({
  getAuditLogs: (...args: any[]) => mockGetAuditLogs(...args),
}))

vi.mock('@/components/ui/PageHeader.vue', () => ({
  default: { name: 'PageHeader', template: '<div class="page-header" />' },
}))

vi.mock('@/components/ui/GlassCard.vue', () => ({
  default: { name: 'GlassCard', template: '<div class="glass-card"><slot /></div>' },
}))

vi.mock('@/components/ui/StatusBadge.vue', () => ({
  default: { name: 'StatusBadge', template: '<span class="status-badge" />' },
}))

describe('AuditLog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAuditLogs.mockResolvedValue({
      data: {
        content: [
          {
            id: 1,
            action: 'LOGIN',
            targetType: 'USER',
            targetId: 1,
            detail: 'User logged in',
            ip: '127.0.0.1',
            success: true,
            createdAt: '2024-01-01T00:00:00Z',
          },
          {
            id: 2,
            action: 'DELETE_DOCUMENT',
            targetType: 'DOCUMENT',
            targetId: 5,
            detail: 'Deleted document',
            ip: '192.168.1.1',
            success: false,
            createdAt: '2024-01-02T00:00:00Z',
          },
        ],
        totalElements: 2,
      },
    })
  })

  it('renders the page container', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    expect(wrapper.find('.page-container').exists()).toBe(true)
  })

  it('renders page header', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    const header = wrapper.findComponent({ name: 'PageHeader' })
    expect(header.exists()).toBe(true)
  })

  it('renders glass card', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    const card = wrapper.findComponent({ name: 'GlassCard' })
    expect(card.exists()).toBe(true)
  })

  it('loads audit logs on mount', async () => {
    mount(AuditLog)
    await flushPromises()
    expect(mockGetAuditLogs).toHaveBeenCalledWith(0, 20)
  })

  it('renders audit log table', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    const table = wrapper.findComponent({ name: 'ElTable' })
    expect(table.exists()).toBe(true)
  })

  it('renders table columns', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    const columns = wrapper.findAllComponents({ name: 'ElTableColumn' })
    expect(columns.length).toBeGreaterThanOrEqual(5)
  })

  it('renders action column data', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    expect(wrapper.text()).toContain('LOGIN')
    expect(wrapper.text()).toContain('DELETE_DOCUMENT')
  })

  it('renders target type column data', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    expect(wrapper.text()).toContain('USER')
    expect(wrapper.text()).toContain('DOCUMENT')
  })

  it('renders IP column data', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    expect(wrapper.text()).toContain('127.0.0.1')
    expect(wrapper.text()).toContain('192.168.1.1')
  })

  it('renders status badges', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    const badges = wrapper.findAllComponents({ name: 'StatusBadge' })
    expect(badges.length).toBeGreaterThanOrEqual(2)
  })

  it('renders pagination when total > 0', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    const pagination = wrapper.findComponent({ name: 'ElPagination' })
    expect(pagination.exists()).toBe(true)
  })

  it('shows empty state when no logs', async () => {
    mockGetAuditLogs.mockResolvedValue({
      data: { content: [], totalElements: 0 },
    })
    const wrapper = mount(AuditLog)
    await flushPromises()
    expect(wrapper.text()).toContain('暂无审计日志')
  })

  it('formats timestamps', async () => {
    const wrapper = mount(AuditLog)
    await flushPromises()
    // The formatted time should contain date parts
    expect(wrapper.text()).toContain('2024')
  })
})
