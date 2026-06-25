import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ChatHeader from '@/components/chat/ChatHeader.vue'

describe('ChatHeader', () => {
  const defaultProps = {
    apiKeys: [
      { id: 1, name: 'Key 1' },
      { id: 2, name: 'Key 2' },
    ],
    selectedApiKeyId: undefined,
  }

  it('renders the header title', () => {
    const wrapper = mount(ChatHeader, { props: defaultProps })
    expect(wrapper.find('.title-text').text()).toBe('智能对话')
  })

  it('renders AI icon', () => {
    const wrapper = mount(ChatHeader, { props: defaultProps })
    expect(wrapper.find('.title-icon span').text()).toBe('AI')
  })

  it('renders API key select options', () => {
    const wrapper = mount(ChatHeader, { props: defaultProps })
    const select = wrapper.findComponent({ name: 'ElSelect' })
    expect(select.exists()).toBe(true)
  })

  it('renders new chat button', () => {
    const wrapper = mount(ChatHeader, { props: defaultProps })
    const btn = wrapper.find('.new-chat-btn')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('新对话')
  })

  it('emits newChat when button is clicked', async () => {
    const wrapper = mount(ChatHeader, { props: defaultProps })
    await wrapper.find('.new-chat-btn').trigger('click')
    expect(wrapper.emitted('newChat')).toHaveLength(1)
  })

  it('shows RAG tag when API key is selected', () => {
    const wrapper = mount(ChatHeader, {
      props: { ...defaultProps, selectedApiKeyId: 1 },
    })
    expect(wrapper.find('.rag-tag').exists()).toBe(true)
    expect(wrapper.find('.rag-tag').text()).toContain('RAG 模式')
  })

  it('does not show RAG tag when no API key selected', () => {
    const wrapper = mount(ChatHeader, { props: defaultProps })
    expect(wrapper.find('.rag-tag').exists()).toBe(false)
  })

  it('emits update:selectedApiKeyId when select changes', async () => {
    const wrapper = mount(ChatHeader, { props: defaultProps })
    const select = wrapper.findComponent({ name: 'ElSelect' })
    await select.vm.$emit('update:modelValue', 2)
    expect(wrapper.emitted('update:selectedApiKeyId')).toBeTruthy()
    expect(wrapper.emitted('update:selectedApiKeyId')![0]).toEqual([2])
  })
})
