import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import KnowledgeList from '@/views/knowledge/KnowledgeList.vue'

const mockPush = vi.fn()
const mockGetKnowledgeBases = vi.fn()
const mockCreateKnowledgeBase = vi.fn()
const mockDeleteKnowledgeBase = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ query: {}, params: {} }),
}))

vi.mock('@/api/knowledge', () => ({
  getKnowledgeBases: (...args: any[]) => mockGetKnowledgeBases(...args),
  createKnowledgeBase: (...args: any[]) => mockCreateKnowledgeBase(...args),
  deleteKnowledgeBase: (...args: any[]) => mockDeleteKnowledgeBase(...args),
}))

describe('KnowledgeList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetKnowledgeBases.mockResolvedValue({
      data: [
        { id: 1, name: 'KB 1', description: 'Description 1', status: 'active', documentCount: 5, chunkCount: 50, createdAt: '2024-01-01T00:00:00Z' },
        { id: 2, name: 'KB 2', description: 'Description 2', status: 'active', documentCount: 3, chunkCount: 30, createdAt: '2024-01-02T00:00:00Z' },
      ],
    })
  })

  it('renders the page container', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.find('.page-container').exists()).toBe(true)
  })

  it('renders page header', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('知识库管理')
  })

  it('renders create button', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('创建知识库')
  })

  it('loads knowledge bases on mount', async () => {
    mount(KnowledgeList)
    await flushPromises()
    expect(mockGetKnowledgeBases).toHaveBeenCalled()
  })

  it('renders knowledge base cards', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    const cards = wrapper.findAll('.kb-card')
    expect(cards).toHaveLength(2)
  })

  it('renders knowledge base names', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('KB 1')
    expect(wrapper.text()).toContain('KB 2')
  })

  it('renders knowledge base descriptions', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('Description 1')
    expect(wrapper.text()).toContain('Description 2')
  })

  it('renders document count for each KB', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('3')
  })

  it('renders status badges', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('启用')
  })

  it('shows empty state when no knowledge bases', async () => {
    mockGetKnowledgeBases.mockResolvedValue({ data: [] })
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('暂无知识库')
  })

  it('renders create dialog', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    expect(dialog.exists()).toBe(true)
  })

  it('navigates to KB detail on card click', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()

    const card = wrapper.find('.kb-card')
    await card.trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/knowledge/1')
  })

  it('renders manage and delete action buttons', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('管理')
    expect(wrapper.text()).toContain('删除')
  })

  it('renders header subtitle', async () => {
    const wrapper = mount(KnowledgeList)
    await flushPromises()
    expect(wrapper.text()).toContain('管理您的智能知识库')
  })
})
