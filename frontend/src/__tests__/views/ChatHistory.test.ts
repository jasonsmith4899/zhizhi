import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ChatHistory from '@/views/chat/ChatHistory.vue'

const mockGetConversations = vi.fn()
const mockGetMessages = vi.fn()
const mockDeleteConversation = vi.fn()

vi.mock('@/api/chat', () => ({
  getConversations: (...args: any[]) => mockGetConversations(...args),
  getMessages: (...args: any[]) => mockGetMessages(...args),
  deleteConversation: (...args: any[]) => mockDeleteConversation(...args),
}))

vi.mock('@/utils/markdown', () => ({
  renderMarkdown: (text: string) => `<p>${text}</p>`,
}))

describe('ChatHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetConversations.mockResolvedValue({
      data: [
        { id: 1, title: 'Conv 1', messageCount: 5, updatedAt: '2024-01-01T00:00:00Z' },
        { id: 2, title: 'Conv 2', messageCount: 3, updatedAt: '2024-01-02T00:00:00Z' },
      ],
    })
    mockGetMessages.mockResolvedValue({
      data: [
        { id: 1, role: 'user', content: 'Hello', createdAt: '2024-01-01T00:00:00Z' },
        { id: 2, role: 'assistant', content: 'Hi there!', createdAt: '2024-01-01T00:00:01Z' },
      ],
    })
  })

  it('renders the page container', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()
    expect(wrapper.find('.page-container').exists()).toBe(true)
  })

  it('renders page header', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()
    expect(wrapper.text()).toContain('对话记录')
  })

  it('renders conversation list', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()
    expect(wrapper.text()).toContain('会话列表')
  })

  it('loads conversations on mount', async () => {
    mount(ChatHistory)
    await flushPromises()
    expect(mockGetConversations).toHaveBeenCalled()
  })

  it('renders conversation items', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()
    const convItems = wrapper.findAll('.conv-item')
    expect(convItems).toHaveLength(2)
  })

  it('renders conversation titles', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()
    expect(wrapper.text()).toContain('Conv 1')
    expect(wrapper.text()).toContain('Conv 2')
  })

  it('shows empty state when no conversations', async () => {
    mockGetConversations.mockResolvedValue({ data: [] })
    const wrapper = mount(ChatHistory)
    await flushPromises()
    expect(wrapper.text()).toContain('暂无对话')
  })

  it('loads messages when conversation is clicked', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()

    const convItem = wrapper.find('.conv-item')
    await convItem.trigger('click')
    await flushPromises()

    expect(mockGetMessages).toHaveBeenCalledWith(1)
  })

  it('renders message details after selecting conversation', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()

    const convItem = wrapper.find('.conv-item')
    await convItem.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Hello')
    expect(wrapper.text()).toContain('Hi there!')
  })

  it('shows prompt to select conversation when none selected', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()
    expect(wrapper.text()).toContain('请从左侧选择一个对话')
  })

  it('renders delete buttons for conversations', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()
    const deleteButtons = wrapper.findAllComponents({ name: 'ElButton' })
      .filter(b => b.text().includes('删除'))
    expect(deleteButtons.length).toBeGreaterThan(0)
  })

  it('renders row layout with two columns', async () => {
    const wrapper = mount(ChatHistory)
    await flushPromises()
    const row = wrapper.findComponent({ name: 'ElRow' })
    expect(row.exists()).toBe(true)
  })
})
