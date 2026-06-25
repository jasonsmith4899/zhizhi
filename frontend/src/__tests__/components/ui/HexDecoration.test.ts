import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HexDecoration from '@/components/ui/HexDecoration.vue'

describe('HexDecoration', () => {
  it('renders slot content', () => {
    const wrapper = mount(HexDecoration, {
      slots: { default: '<span class="inner">icon</span>' },
    })
    expect(wrapper.find('.inner').text()).toBe('icon')
  })

  it('applies default classes: size-md, is-float', () => {
    const wrapper = mount(HexDecoration)
    expect(wrapper.classes()).toContain('size-md')
    expect(wrapper.classes()).toContain('is-float')
  })

  it('applies size-sm class', () => {
    const wrapper = mount(HexDecoration, { props: { size: 'sm' } })
    expect(wrapper.classes()).toContain('size-sm')
  })

  it('applies size-lg class', () => {
    const wrapper = mount(HexDecoration, { props: { size: 'lg' } })
    expect(wrapper.classes()).toContain('size-lg')
  })

  it('does not apply is-float when float is false', () => {
    const wrapper = mount(HexDecoration, { props: { float: false } })
    expect(wrapper.classes()).not.toContain('is-float')
  })

  it('applies opacity style for corner variant', () => {
    const wrapper = mount(HexDecoration, {
      props: { variant: 'corner', opacity: 0.2 },
    })
    expect(wrapper.attributes('style')).toContain('opacity: 0.2')
  })

  it('applies opacity 1 for non-corner variant', () => {
    const wrapper = mount(HexDecoration, {
      props: { variant: 'floating' },
    })
    expect(wrapper.attributes('style')).toContain('opacity: 1')
  })

  it('applies opacity 1 for icon variant', () => {
    const wrapper = mount(HexDecoration, {
      props: { variant: 'icon' },
    })
    expect(wrapper.attributes('style')).toContain('opacity: 1')
  })
})
