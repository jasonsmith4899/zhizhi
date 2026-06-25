import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import KgRelationTable from '@/components/kg/KgRelationTable.vue'

const mockGetKgRelations = vi.fn()

vi.mock('@/api/kg', () => ({
  getKgRelations: (...args: any[]) => mockGetKgRelations(...args),
}))

vi.mock('@/components/ui/ScoreTag.vue', () => ({
  default: { name: 'ScoreTag', template: '<span class="score-tag" />' },
}))

describe('KgRelationTable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetKgRelations.mockResolvedValue({
      data: {
        content: [
          {
            id: 1,
            sourceName: 'Entity A',
            targetName: 'Entity B',
            predicate: 'relates_to',
            confidence: 0.95,
            documentId: 1,
            documentName: 'Doc1.pdf',
            createdAt: '2024-01-01T00:00:00Z',
          },
        ],
        totalElements: 1,
      },
    })
  })

  it('renders search input', async () => {
    const wrapper = mount(KgRelationTable, { props: { kbId: 1 } })
    await flushPromises()
    const input = wrapper.findComponent({ name: 'ElInput' })
    expect(input.exists()).toBe(true)
  })

  it('renders confidence filter select', async () => {
    const wrapper = mount(KgRelationTable, { props: { kbId: 1 } })
    await flushPromises()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    expect(selects.length).toBeGreaterThanOrEqual(1)
  })

  it('loads relations on mount', async () => {
    mount(KgRelationTable, { props: { kbId: 1 } })
    await flushPromises()
    expect(mockGetKgRelations).toHaveBeenCalledWith(1, expect.objectContaining({
      page: 0,
      size: 20,
    }))
  })

  it('renders table with relation data', async () => {
    const wrapper = mount(KgRelationTable, { props: { kbId: 1 } })
    await flushPromises()
    const table = wrapper.findComponent({ name: 'ElTable' })
    expect(table.exists()).toBe(true)
  })

  it('renders pagination', async () => {
    const wrapper = mount(KgRelationTable, { props: { kbId: 1 } })
    await flushPromises()
    const pagination = wrapper.findComponent({ name: 'ElPagination' })
    expect(pagination.exists()).toBe(true)
  })

  it('reloads relations when kbId changes', async () => {
    const wrapper = mount(KgRelationTable, { props: { kbId: 1 } })
    await flushPromises()
    expect(mockGetKgRelations).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ kbId: 2 })
    await flushPromises()
    expect(mockGetKgRelations).toHaveBeenCalledTimes(2)
  })

  it('renders refresh button', async () => {
    const wrapper = mount(KgRelationTable, { props: { kbId: 1 } })
    await flushPromises()
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    expect(buttons.length).toBeGreaterThanOrEqual(1)
  })
})
