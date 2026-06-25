import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ChatInput from '@/components/chat/ChatInput.vue'

describe('ChatInput', () => {
  it('renders textarea and send button', () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    expect(wrapper.find('.message-input').exists()).toBe(true)
    expect(wrapper.find('.send-btn').exists()).toBe(true)
  })

  it('renders placeholder text', () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    const textarea = wrapper.findComponent({ name: 'ElInput' })
    expect(textarea.exists()).toBe(true)
  })

  it('emits send event with trimmed text on button click', async () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    const textarea = wrapper.findComponent({ name: 'ElInput' })

    // Set input value via the component's v-model
    await textarea.setValue('  hello world  ')
    await wrapper.find('.send-btn').trigger('click')

    expect(wrapper.emitted('send')).toBeTruthy()
    expect(wrapper.emitted('send')![0]).toEqual(['hello world'])
  })

  it('does not emit send when input is empty', async () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    await wrapper.find('.send-btn').trigger('click')
    expect(wrapper.emitted('send')).toBeFalsy()
  })

  it('does not emit send when input is whitespace only', async () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    const textarea = wrapper.findComponent({ name: 'ElInput' })
    await textarea.setValue('   ')
    await wrapper.find('.send-btn').trigger('click')
    expect(wrapper.emitted('send')).toBeFalsy()
  })

  it('clears input after sending', async () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    const textarea = wrapper.findComponent({ name: 'ElInput' })
    await textarea.setValue('test message')
    await wrapper.find('.send-btn').trigger('click')

    // Input should be cleared
    expect(wrapper.vm.input).toBe('')
  })

  it('exposes clear method', () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    expect(typeof wrapper.vm.clear).toBe('function')
  })

  it('clear method resets input value', async () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    wrapper.vm.input = 'some text'
    wrapper.vm.clear()
    expect(wrapper.vm.input).toBe('')
  })

  it('emits send on Enter key (without Shift)', async () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    const textarea = wrapper.findComponent({ name: 'ElInput' })
    await textarea.setValue('hello')

    const textareaEl = wrapper.find('textarea')
    if (textareaEl.exists()) {
      await textareaEl.trigger('keydown', { key: 'Enter', shiftKey: false })
    }

    expect(wrapper.emitted('send')).toBeTruthy()
    expect(wrapper.emitted('send')![0]).toEqual(['hello'])
  })

  it('does not emit send on Shift+Enter', async () => {
    const wrapper = mount(ChatInput, { props: { loading: false } })
    const textarea = wrapper.findComponent({ name: 'ElInput' })
    await textarea.setValue('hello')

    const textareaEl = wrapper.find('textarea')
    if (textareaEl.exists()) {
      await textareaEl.trigger('keydown', { key: 'Enter', shiftKey: true })
    }

    expect(wrapper.emitted('send')).toBeFalsy()
  })

  it('disables input when loading', () => {
    const wrapper = mount(ChatInput, { props: { loading: true } })
    const textarea = wrapper.findComponent({ name: 'ElInput' })
    expect(textarea.props('disabled')).toBe(true)
  })

  it('shows loading state on send button when loading', () => {
    const wrapper = mount(ChatInput, { props: { loading: true } })
    const btn = wrapper.findComponent({ name: 'ElButton' })
    expect(btn.props('loading')).toBe(true)
  })
})
