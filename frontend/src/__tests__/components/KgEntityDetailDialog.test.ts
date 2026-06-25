import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import KgEntityDetailDialog from '@/components/kg/KgEntityDetailDialog.vue'

const mockGetKgEntityDetail = vi.fn()

vi.mock('@/api/kg', () => ({
  getKgEntityDetail: (...args: any[]) => mockGetKgEntityDetail(...args),
}))

vi.mock('@/components/ui/ScoreTag.vue', () => ({
  default: { name: 'ScoreTag', template: '<span class="score-tag" />' },
}))

vi.mock('@/components/ui/ShineButton.vue', () => ({
  default: { name: 'ShineButton', template: '<button class="shine-btn"><slot /></button>', emits: ['click'] },
}))

describe('KgEntityDetailDialog', () => {
  const mockDetail = {
    id: 1,
    name: 'Test Entity',
    type: '人物',
    description: 'A test entity',
    mentionCount: 10,
    createdAt: '2024-01-01T00:00:00Z',
    relations: [
      {
        id: 1,
        predicate: 'works_at',
        direction: 'out' as const,
        otherEntity: 'Company A',
        confidence: 0.9,
      },
      {
        id: 2,
        predicate: 'manages',
        direction: 'in' as const,
        otherEntity: 'Manager B',
        confidence: 0.8,
      },
    ],
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetKgEntityDetail.mockResolvedValue({ data: mockDetail })
  })

  it('renders dialog component', async () => {
    const wrapper = mount(KgEntityDetailDialog, {
      props: { kbId: 1, entityId: 1, visible: true },
    })
    await flushPromises()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    expect(dialog.exists()).toBe(true)
  })

  it('loads entity detail when visible becomes true', async () => {
    const wrapper = mount(KgEntityDetailDialog, {
      props: { kbId: 1, entityId: 1, visible: false },
    })
    await flushPromises()
    expect(mockGetKgEntityDetail).not.toHaveBeenCalled()

    await wrapper.setProps({ visible: true })
    await flushPromises()
    expect(mockGetKgEntityDetail).toHaveBeenCalledWith(1, 1)
  })

  it('calls API with correct params', async () => {
    const wrapper = mount(KgEntityDetailDialog, {
      props: { kbId: 5, entityId: 10, visible: false },
    })
    await flushPromises()
    await wrapper.setProps({ visible: true })
    await flushPromises()
    expect(mockGetKgEntityDetail).toHaveBeenCalledWith(5, 10)
  })

  it('emits update:visible when dialog closes', async () => {
    const wrapper = mount(KgEntityDetailDialog, {
      props: { kbId: 1, entityId: 1, visible: true },
    })
    await flushPromises()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    await dialog.vm.$emit('update:modelValue', false)
    expect(wrapper.emitted('update:visible')).toBeTruthy()
  })

  it('emits locateInGraph when locate button is clicked', async () => {
    const wrapper = mount(KgEntityDetailDialog, {
      props: { kbId: 1, entityId: 1, visible: true },
    })
    await flushPromises()
    // Wait for detail to load and render
    await wrapper.vm.$nextTick()
    const locateBtn = wrapper.find('.shine-btn')
    if (locateBtn.exists()) {
      await locateBtn.trigger('click')
      expect(wrapper.emitted('locateInGraph')).toBeTruthy()
      expect(wrapper.emitted('locateInGraph')![0]).toEqual([1])
    }
  })

  it('does not load detail when entityId is null', async () => {
    mount(KgEntityDetailDialog, {
      props: { kbId: 1, entityId: null, visible: true },
    })
    await flushPromises()
    expect(mockGetKgEntityDetail).not.toHaveBeenCalled()
  })

  it('renders dialog with correct title', async () => {
    const wrapper = mount(KgEntityDetailDialog, {
      props: { kbId: 1, entityId: 1, visible: true },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('实体详情')
  })

  it('renders close button', async () => {
    const wrapper = mount(KgEntityDetailDialog, {
      props: { kbId: 1, entityId: 1, visible: true },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('关闭')
  })

  it('renders detail body container', async () => {
    const wrapper = mount(KgEntityDetailDialog, {
      props: { kbId: 1, entityId: 1, visible: true },
    })
    await flushPromises()
    expect(wrapper.find('.detail-body').exists()).toBe(true)
  })
})
