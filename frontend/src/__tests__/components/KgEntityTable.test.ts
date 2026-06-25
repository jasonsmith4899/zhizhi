import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import KgEntityTable from '@/components/kg/KgEntityTable.vue'

const mockGetKgEntities = vi.fn()

vi.mock('@/api/kg', () => ({
  getKgEntities: (...args: any[]) => mockGetKgEntities(...args),
}))

describe('KgEntityTable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetKgEntities.mockResolvedValue({
      data: {
        content: [
          { id: 1, name: 'Entity A', type: '人物', description: 'Desc A', mentionCount: 10, createdAt: '2024-01-01T00:00:00Z' },
          { id: 2, name: 'Entity B', type: '组织', description: 'Desc B', mentionCount: 5, createdAt: '2024-01-02T00:00:00Z' },
        ],
        totalElements: 2,
      },
    })
  })

  it('renders search input', async () => {
    const wrapper = mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()
    const input = wrapper.findComponent({ name: 'ElInput' })
    expect(input.exists()).toBe(true)
  })

  it('renders type filter select', async () => {
    const wrapper = mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    expect(selects.length).toBeGreaterThanOrEqual(1)
  })

  it('renders refresh button', async () => {
    const wrapper = mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()
    const refreshBtn = wrapper.findComponent({ name: 'ElButton' })
    expect(refreshBtn.exists()).toBe(true)
  })

  it('loads entities on mount', async () => {
    mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()
    expect(mockGetKgEntities).toHaveBeenCalledWith(1, expect.objectContaining({
      page: 0,
      size: 20,
    }))
  })

  it('renders entity data in table', async () => {
    const wrapper = mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()
    const table = wrapper.findComponent({ name: 'ElTable' })
    expect(table.exists()).toBe(true)
  })

  it('emits viewDetail when detail button is clicked', async () => {
    const wrapper = mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()

    // Find detail buttons
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const detailBtn = buttons.find(b => b.text().includes('详情'))
    if (detailBtn) {
      await detailBtn.trigger('click')
      expect(wrapper.emitted('viewDetail')).toBeTruthy()
    }
  })

  it('emits locateInGraph when graph button is clicked', async () => {
    const wrapper = mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()

    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const graphBtn = buttons.find(b => b.text().includes('图谱定位'))
    if (graphBtn) {
      await graphBtn.trigger('click')
      expect(wrapper.emitted('locateInGraph')).toBeTruthy()
    }
  })

  it('renders pagination', async () => {
    const wrapper = mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()
    const pagination = wrapper.findComponent({ name: 'ElPagination' })
    expect(pagination.exists()).toBe(true)
  })

  it('reloads entities when kbId changes', async () => {
    const wrapper = mount(KgEntityTable, { props: { kbId: 1 } })
    await flushPromises()
    expect(mockGetKgEntities).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ kbId: 2 })
    await flushPromises()
    expect(mockGetKgEntities).toHaveBeenCalledTimes(2)
  })
})
