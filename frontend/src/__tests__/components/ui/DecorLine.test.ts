import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DecorLine from '@/components/ui/DecorLine.vue'

describe('DecorLine', () => {
  it('renders a span element', () => {
    const wrapper = mount(DecorLine)
    expect(wrapper.find('span').exists()).toBe(true)
  })

  it('applies default variant-gradient class', () => {
    const wrapper = mount(DecorLine)
    expect(wrapper.classes()).toContain('variant-gradient')
  })

  it('applies variant-fade class when variant is fade', () => {
    const wrapper = mount(DecorLine, { props: { variant: 'fade' } })
    expect(wrapper.classes()).toContain('variant-fade')
  })

  it('applies variant-sweep class when variant is sweep', () => {
    const wrapper = mount(DecorLine, { props: { variant: 'sweep' } })
    expect(wrapper.classes()).toContain('variant-sweep')
  })

  it('applies default height of 2px', () => {
    const wrapper = mount(DecorLine)
    expect(wrapper.attributes('style')).toContain('height: 2px')
  })

  it('applies custom height', () => {
    const wrapper = mount(DecorLine, { props: { height: 5 } })
    expect(wrapper.attributes('style')).toContain('height: 5px')
  })
})
