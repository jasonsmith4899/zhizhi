import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownBody from '@/components/ui/MarkdownBody.vue'

vi.mock('@/utils/markdown', () => ({
  renderMarkdown: (text: string) => {
    if (!text) return ''
    return `<p>${text}</p>`
  },
}))

describe('MarkdownBody', () => {
  it('renders content as HTML', () => {
    const wrapper = mount(MarkdownBody, {
      props: { content: 'Hello World' },
    })
    expect(wrapper.find('p').text()).toBe('Hello World')
  })

  it('applies default class markdown-body with size md', () => {
    const wrapper = mount(MarkdownBody, {
      props: { content: 'test' },
    })
    expect(wrapper.classes()).toContain('markdown-body')
    expect(wrapper.classes()).toContain('md')
  })

  it('applies size-sm class', () => {
    const wrapper = mount(MarkdownBody, {
      props: { content: 'test', size: 'sm' },
    })
    expect(wrapper.classes()).toContain('sm')
  })

  it('applies boxed class when boxed is true', () => {
    const wrapper = mount(MarkdownBody, {
      props: { content: 'test', boxed: true },
    })
    expect(wrapper.classes()).toContain('boxed')
  })

  it('does not apply boxed class by default', () => {
    const wrapper = mount(MarkdownBody, {
      props: { content: 'test' },
    })
    expect(wrapper.classes()).not.toContain('boxed')
  })

  it('renders empty HTML for empty content', () => {
    const wrapper = mount(MarkdownBody, {
      props: { content: '' },
    })
    expect(wrapper.html()).not.toContain('<p>')
  })
})
