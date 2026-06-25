import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import KgGraphDialog from '@/components/kg/KgGraphDialog.vue'

const mockGetKgGraph = vi.fn()
const mockGetKgEntityDetail = vi.fn()
const mockSearchKgEntities = vi.fn()

vi.mock('@/api/kg', () => ({
  getKgGraph: (...args: any[]) => mockGetKgGraph(...args),
  getKgEntityDetail: (...args: any[]) => mockGetKgEntityDetail(...args),
  searchKgEntities: (...args: any[]) => mockSearchKgEntities(...args),
}))

// Mock vis-network
vi.mock('vis-network/standalone', () => ({
  Network: vi.fn().mockImplementation(() => ({
    on: vi.fn(),
    destroy: vi.fn(),
    focus: vi.fn(),
    selectNodes: vi.fn(),
    getConnectedNodes: vi.fn().mockReturnValue([]),
    getConnectedEdges: vi.fn().mockReturnValue([]),
  })),
  DataSet: vi.fn().mockImplementation((items: any[]) => ({
    getIds: vi.fn().mockReturnValue(items.map((_: any, i: number) => i)),
    update: vi.fn(),
    add: vi.fn(),
    remove: vi.fn(),
  })),
}))

afterEach(() => {
  document.body.innerHTML = ''
})

describe('KgGraphDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetKgGraph.mockResolvedValue({
      data: {
        nodes: [
          { id: 1, name: 'Node A', type: '人物', mentionCount: 5 },
          { id: 2, name: 'Node B', type: '组织', mentionCount: 3 },
        ],
        edges: [
          { source: 1, target: 2, predicate: 'works_at', confidence: 0.9 },
        ],
      },
    })
    mockGetKgEntityDetail.mockResolvedValue({
      data: {
        id: 1,
        name: 'Node A',
        type: '人物',
        description: 'Desc',
        mentionCount: 5,
        createdAt: '2024-01-01T00:00:00Z',
        relations: [],
      },
    })
  })

  it('renders dialog when visible is true', async () => {
    const wrapper = mount(KgGraphDialog, {
      props: { visible: true, kbId: 1 },
    })
    await nextTick()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    expect(dialog.exists()).toBe(true)
  })

  it('renders toolbar with title', async () => {
    const wrapper = mount(KgGraphDialog, {
      props: { visible: true, kbId: 1 },
    })
    await nextTick()
    const title = wrapper.find('.graph-title')
    expect(title.exists()).toBe(true)
    expect(title.text()).toContain('图谱可视化')
  })

  it('renders search input', async () => {
    const wrapper = mount(KgGraphDialog, {
      props: { visible: true, kbId: 1 },
    })
    await nextTick()
    const input = wrapper.findComponent({ name: 'ElInput' })
    expect(input.exists()).toBe(true)
  })

  it('renders close button', async () => {
    const wrapper = mount(KgGraphDialog, {
      props: { visible: true, kbId: 1 },
    })
    await nextTick()
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const closeBtn = buttons.find(b => b.text().includes('关闭'))
    expect(closeBtn).toBeTruthy()
  })

  it('emits update:visible when close button is clicked', async () => {
    const wrapper = mount(KgGraphDialog, {
      props: { visible: true, kbId: 1 },
    })
    await nextTick()
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const closeBtn = buttons.find(b => b.text().includes('关闭'))
    if (closeBtn) {
      await closeBtn.trigger('click')
      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
    }
  })

  it('renders graph canvas element', async () => {
    const wrapper = mount(KgGraphDialog, {
      props: { visible: true, kbId: 1 },
    })
    await nextTick()
    expect(wrapper.find('.graph-canvas').exists()).toBe(true)
  })

  it('loads graph data when visible becomes true', async () => {
    const wrapper = mount(KgGraphDialog, {
      props: { visible: false, kbId: 1 },
    })
    await flushPromises()
    expect(mockGetKgGraph).not.toHaveBeenCalled()

    await wrapper.setProps({ visible: true })
    await flushPromises()
    expect(mockGetKgGraph).toHaveBeenCalled()
  })

  it('renders fullscreen dialog', async () => {
    const wrapper = mount(KgGraphDialog, {
      props: { visible: true, kbId: 1 },
    })
    await nextTick()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    expect(dialog.props('fullscreen')).toBe(true)
  })
})
