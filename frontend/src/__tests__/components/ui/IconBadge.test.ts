import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import IconBadge from '@/components/ui/IconBadge.vue'

describe('IconBadge', () => {
  it('renders slot content', () => {
    const wrapper = mount(IconBadge, {
      slots: { default: '<span class="icon-inner">X</span>' },
    })
    expect(wrapper.find('.icon-inner').text()).toBe('X')
  })

  it('applies default classes: size-md, variant-gradient, radius-icon', () => {
    const wrapper = mount(IconBadge)
    expect(wrapper.classes()).toContain('size-md')
    expect(wrapper.classes()).toContain('variant-gradient')
    expect(wrapper.classes()).toContain('radius-icon')
  })

  it('applies size-sm class', () => {
    const wrapper = mount(IconBadge, { props: { size: 'sm' } })
    expect(wrapper.classes()).toContain('size-sm')
  })

  it('applies size-lg class', () => {
    const wrapper = mount(IconBadge, { props: { size: 'lg' } })
    expect(wrapper.classes()).toContain('size-lg')
  })

  it('applies size-xl class', () => {
    const wrapper = mount(IconBadge, { props: { size: 'xl' } })
    expect(wrapper.classes()).toContain('size-xl')
  })

  it('applies variant-ghost class', () => {
    const wrapper = mount(IconBadge, { props: { variant: 'ghost' } })
    expect(wrapper.classes()).toContain('variant-ghost')
  })

  it('applies variant-ghost-danger class', () => {
    const wrapper = mount(IconBadge, { props: { variant: 'ghost-danger' } })
    expect(wrapper.classes()).toContain('variant-ghost-danger')
  })

  it('applies radius-md class', () => {
    const wrapper = mount(IconBadge, { props: { radius: 'md' } })
    expect(wrapper.classes()).toContain('radius-md')
  })

  it('applies radius-button class', () => {
    const wrapper = mount(IconBadge, { props: { radius: 'button' } })
    expect(wrapper.classes()).toContain('radius-button')
  })

  it('applies radius-pill class', () => {
    const wrapper = mount(IconBadge, { props: { radius: 'pill' } })
    expect(wrapper.classes()).toContain('radius-pill')
  })

  it('does not render glow span by default', () => {
    const wrapper = mount(IconBadge)
    expect(wrapper.find('.glow').exists()).toBe(false)
  })

  it('renders glow span when glow is true', () => {
    const wrapper = mount(IconBadge, { props: { glow: true } })
    expect(wrapper.find('.glow').exists()).toBe(true)
  })

  it('does not render ring span by default', () => {
    const wrapper = mount(IconBadge)
    expect(wrapper.find('.ring').exists()).toBe(false)
  })

  it('renders ring span when pulseRing is true', () => {
    const wrapper = mount(IconBadge, { props: { pulseRing: true } })
    expect(wrapper.find('.ring').exists()).toBe(true)
  })

  it('renders content wrapper', () => {
    const wrapper = mount(IconBadge, {
      slots: { default: 'icon' },
    })
    expect(wrapper.find('.content').exists()).toBe(true)
  })
})
