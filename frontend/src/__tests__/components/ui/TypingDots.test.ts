import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TypingDots from '@/components/ui/TypingDots.vue'

describe('TypingDots', () => {
  it('renders three dot spans', () => {
    const wrapper = mount(TypingDots)
    const dots = wrapper.findAll('span span')
    expect(dots).toHaveLength(3)
  })

  it('renders root typing element', () => {
    const wrapper = mount(TypingDots)
    expect(wrapper.find('.typing').exists()).toBe(true)
  })

  it('applies default dot color via CSS variable', () => {
    const wrapper = mount(TypingDots)
    const style = wrapper.attributes('style')
    expect(style).toContain('--dot: var(--color-neon-blue)')
  })

  it('applies custom dot color', () => {
    const wrapper = mount(TypingDots, { props: { dotColor: 'red' } })
    const style = wrapper.attributes('style')
    expect(style).toContain('--dot: red')
  })

  it('applies default size of 10px', () => {
    const wrapper = mount(TypingDots)
    const style = wrapper.attributes('style')
    expect(style).toContain('--dot-sz: 10px')
  })

  it('applies custom size', () => {
    const wrapper = mount(TypingDots, { props: { size: 16 } })
    const style = wrapper.attributes('style')
    expect(style).toContain('--dot-sz: 16px')
  })
})
