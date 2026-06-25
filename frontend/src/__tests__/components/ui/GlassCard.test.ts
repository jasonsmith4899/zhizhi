import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import GlassCard from '@/components/ui/GlassCard.vue'

describe('GlassCard', () => {
  it('renders default slot content', () => {
    const wrapper = mount(GlassCard, {
      slots: { default: '<p class="inner">Hello</p>' },
    })
    expect(wrapper.find('.inner').text()).toBe('Hello')
  })

  it('applies default classes: lift-none, pad-md, radius-lg', () => {
    const wrapper = mount(GlassCard)
    expect(wrapper.classes()).toContain('lift-none')
    expect(wrapper.classes()).toContain('pad-md')
    expect(wrapper.classes()).toContain('radius-lg')
  })

  it('applies lift-sm class', () => {
    const wrapper = mount(GlassCard, { props: { lift: 'sm' } })
    expect(wrapper.classes()).toContain('lift-sm')
  })

  it('applies lift-lg class', () => {
    const wrapper = mount(GlassCard, { props: { lift: 'lg' } })
    expect(wrapper.classes()).toContain('lift-lg')
  })

  it('applies pad-none class', () => {
    const wrapper = mount(GlassCard, { props: { padding: 'none' } })
    expect(wrapper.classes()).toContain('pad-none')
  })

  it('applies pad-sm class', () => {
    const wrapper = mount(GlassCard, { props: { padding: 'sm' } })
    expect(wrapper.classes()).toContain('pad-sm')
  })

  it('applies pad-lg class', () => {
    const wrapper = mount(GlassCard, { props: { padding: 'lg' } })
    expect(wrapper.classes()).toContain('pad-lg')
  })

  it('applies radius-xl class', () => {
    const wrapper = mount(GlassCard, { props: { radius: 'xl' } })
    expect(wrapper.classes()).toContain('radius-xl')
  })

  it('does not render top-bar by default', () => {
    const wrapper = mount(GlassCard)
    expect(wrapper.find('.top-bar').exists()).toBe(false)
  })

  it('renders top-bar when topBar is true', () => {
    const wrapper = mount(GlassCard, { props: { topBar: true } })
    expect(wrapper.find('.top-bar').exists()).toBe(true)
    expect(wrapper.classes()).toContain('has-topbar')
  })

  it('applies is-clickable class when clickable is true', () => {
    const wrapper = mount(GlassCard, { props: { clickable: true } })
    expect(wrapper.classes()).toContain('is-clickable')
  })

  it('applies glow-hover class when glowOnHover is true', () => {
    const wrapper = mount(GlassCard, { props: { glowOnHover: true } })
    expect(wrapper.classes()).toContain('glow-hover')
  })
})
