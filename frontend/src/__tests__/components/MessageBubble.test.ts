import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MessageBubble from '@/components/chat/MessageBubble.vue'

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

describe('MessageBubble', () => {
  const userMessage = {
    role: 'user',
    content: 'Hello, how are you?',
    time: new Date(),
  }

  const assistantMessage = {
    role: 'assistant',
    content: 'I am fine, thank you!',
    time: new Date(),
  }

  const messageWithSources = {
    role: 'assistant',
    content: 'Based on the document...',
    sources: [
      { documentName: 'Doc1.pdf', content: 'Some content', score: 0.95 },
      { documentName: 'Doc2.pdf', content: 'Other content', score: 0.75 },
    ],
    time: new Date(),
  }

  const messageWithKagSources = {
    role: 'assistant',
    content: 'Knowledge graph info',
    kagSources: [
      { sourceName: 'Entity A', predicate: 'relates_to', targetName: 'Entity B', confidence: 0.9 },
    ],
    time: new Date(),
  }

  it('renders user message with correct class', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    expect(wrapper.find('.msg-user').exists()).toBe(true)
    expect(wrapper.find('.user-avatar').exists()).toBe(true)
    expect(wrapper.find('.user-avatar span').text()).toBe('U')
  })

  it('renders assistant message with correct class', () => {
    const wrapper = mount(MessageBubble, { props: { message: assistantMessage } })
    expect(wrapper.find('.msg-ai').exists()).toBe(true)
    expect(wrapper.find('.ai-avatar').exists()).toBe(true)
    expect(wrapper.find('.ai-avatar span').text()).toBe('AI')
  })

  it('renders user message content', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    expect(wrapper.find('.msg-content').exists()).toBe(true)
  })

  it('renders assistant message content', () => {
    const wrapper = mount(MessageBubble, { props: { message: assistantMessage } })
    expect(wrapper.find('.msg-content').exists()).toBe(true)
  })

  it('shows typing indicator when streaming and content is empty', () => {
    const emptyMsg = { role: 'assistant', content: '', time: new Date() }
    const wrapper = mount(MessageBubble, {
      props: { message: emptyMsg, isStreaming: true },
    })
    expect(wrapper.find('.typing-indicator').exists()).toBe(true)
  })

  it('shows streaming cursor when streaming and content exists', () => {
    const wrapper = mount(MessageBubble, {
      props: { message: assistantMessage, isStreaming: true },
    })
    expect(wrapper.find('.streaming-cursor').exists()).toBe(true)
  })

  it('does not show typing indicator when not streaming', () => {
    const wrapper = mount(MessageBubble, { props: { message: assistantMessage } })
    expect(wrapper.find('.typing-indicator').exists()).toBe(false)
    expect(wrapper.find('.streaming-cursor').exists()).toBe(false)
  })

  it('renders sources when present', () => {
    const wrapper = mount(MessageBubble, { props: { message: messageWithSources } })
    expect(wrapper.find('.msg-references').exists()).toBe(true)
    expect(wrapper.findAll('.reference-item')).toHaveLength(2)
  })

  it('renders source document names', () => {
    const wrapper = mount(MessageBubble, { props: { message: messageWithSources } })
    const docNames = wrapper.findAll('.ref-doc-name')
    expect(docNames[0].text()).toBe('Doc1.pdf')
    expect(docNames[1].text()).toBe('Doc2.pdf')
  })

  it('renders source scores', () => {
    const wrapper = mount(MessageBubble, { props: { message: messageWithSources } })
    const scores = wrapper.findAll('.ref-score')
    expect(scores[0].text()).toBe('95%')
    expect(scores[1].text()).toBe('75%')
  })

  it('does not render sources when none present', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    expect(wrapper.find('.msg-references').exists()).toBe(false)
  })

  it('renders KAG sources when present', () => {
    const wrapper = mount(MessageBubble, { props: { message: messageWithKagSources } })
    expect(wrapper.find('.kag-sources').exists()).toBe(true)
    expect(wrapper.find('.kag-source-name').text()).toBe('Entity A')
    expect(wrapper.find('.kag-target-name').text()).toBe('Entity B')
    expect(wrapper.find('.kag-predicate').text()).toContain('relates_to')
    expect(wrapper.find('.kag-confidence').text()).toBe('90%')
  })

  it('does not render KAG sources when none present', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    expect(wrapper.find('.kag-sources').exists()).toBe(false)
  })
})
