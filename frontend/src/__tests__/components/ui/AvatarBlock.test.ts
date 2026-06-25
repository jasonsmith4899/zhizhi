import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AvatarBlock from '@/components/ui/AvatarBlock.vue'

describe('AvatarBlock', () => {
  it('renders text prop', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'AB' } })
    expect(wrapper.find('.text').text()).toBe('AB')
  })

  it('applies default size-md and grad-primary-neon classes', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X' } })
    expect(wrapper.classes()).toContain('size-md')
    expect(wrapper.classes()).toContain('grad-primary-neon')
  })

  it('applies size-sm class when size is sm', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X', size: 'sm' } })
    expect(wrapper.classes()).toContain('size-sm')
  })

  it('applies size-xl class when size is xl', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X', size: 'xl' } })
    expect(wrapper.classes()).toContain('size-xl')
  })

  it('applies grad-purple-primary class when gradient is purple-primary', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X', gradient: 'purple-primary' } })
    expect(wrapper.classes()).toContain('grad-purple-primary')
  })

  it('does not render glow span by default', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X' } })
    expect(wrapper.find('.glow').exists()).toBe(false)
  })

  it('renders glow span when glow is true', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X', glow: true } })
    expect(wrapper.find('.glow').exists()).toBe(true)
  })

  it('does not render online-dot by default', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X' } })
    expect(wrapper.find('.online-dot').exists()).toBe(false)
  })

  it('renders online-dot when online is true', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X', online: true } })
    expect(wrapper.find('.online-dot').exists()).toBe(true)
  })

  it('renders both glow and online-dot when both props are true', () => {
    const wrapper = mount(AvatarBlock, { props: { text: 'X', glow: true, online: true } })
    expect(wrapper.find('.glow').exists()).toBe(true)
    expect(wrapper.find('.online-dot').exists()).toBe(true)
  })
})
