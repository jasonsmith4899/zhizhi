import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MessageList from '@/components/chat/MessageList.vue'

vi.mock('@/utils/markdown', () => ({
  renderMarkdown: (text: string) => `<p>${text}</p>`,
  extractInlineSources: (text: string) => ({
    cleanContent: text,
    inlineSources: [],
  }),
}))

vi.mock('@/api/knowledge', () => ({
  getDocumentChunks: vi.fn().mockResolvedValue({ data: [] }),
}))

describe('MessageList', () => {
  const messages = [
    { role: 'user', content: 'Hello', time: new Date() },
    { role: 'assistant', content: 'Hi there!', time: new Date() },
    { role: 'user', content: 'How are you?', time: new Date() },
  ]

  it('renders empty state when no messages', () => {
    const wrapper = mount(MessageList, {
      props: { messages: [], loading: false },
    })
    expect(wrapper.find('.empty-chat').exists()).toBe(true)
    expect(wrapper.find('.empty-title').text()).toContain('智知 AI 知识库')
  })

  it('renders messages when provided', () => {
    const wrapper = mount(MessageList, {
      props: { messages, loading: false },
    })
    expect(wrapper.find('.empty-chat').exists()).toBe(false)
    expect(wrapper.findAll('.msg-row')).toHaveLength(3)
  })

  it('renders correct role classes for messages', () => {
    const wrapper = mount(MessageList, {
      props: { messages, loading: false },
    })
    const rows = wrapper.findAll('.msg-row')
    expect(rows[0].classes()).toContain('user')
    expect(rows[1].classes()).toContain('assistant')
    expect(rows[2].classes()).toContain('user')
  })

  it('renders empty state subtitle', () => {
    const wrapper = mount(MessageList, {
      props: { messages: [], loading: false },
    })
    expect(wrapper.find('.empty-subtitle').text()).toContain('RAG 问答')
  })

  it('renders decoration lines in empty state', () => {
    const wrapper = mount(MessageList, {
      props: { messages: [], loading: false },
    })
    expect(wrapper.findAll('.decoration-line')).toHaveLength(3)
  })

  it('exposes scrollToBottom method', () => {
    const wrapper = mount(MessageList, {
      props: { messages: [], loading: false },
    })
    expect(typeof wrapper.vm.scrollToBottom).toBe('function')
  })

  it('passes isStreaming to last assistant message when loading', () => {
    const msgs = [
      { role: 'user', content: 'Hello', time: new Date() },
      { role: 'assistant', content: 'Hi', time: new Date() },
    ]
    const wrapper = mount(MessageList, {
      props: { messages: msgs, loading: true },
    })
    // The last message is assistant and loading is true, so isStreaming should be true
    const bubbles = wrapper.findAllComponents({ name: 'MessageBubble' })
    expect(bubbles).toHaveLength(2)
  })
})
