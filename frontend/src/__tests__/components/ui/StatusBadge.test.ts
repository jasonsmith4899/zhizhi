import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBadge from '@/components/ui/StatusBadge.vue'

describe('StatusBadge', () => {
  it('renders label text', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'Active' } })
    expect(wrapper.text()).toContain('Active')
  })

  it('applies default type-success class', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'OK' } })
    expect(wrapper.classes()).toContain('type-success')
  })

  it('applies type-info class', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'Info', type: 'info' } })
    expect(wrapper.classes()).toContain('type-info')
  })

  it('applies type-warning class', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'Warn', type: 'warning' } })
    expect(wrapper.classes()).toContain('type-warning')
  })

  it('applies type-danger class', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'Err', type: 'danger' } })
    expect(wrapper.classes()).toContain('type-danger')
  })

  it('applies type-primary class', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'Pri', type: 'primary' } })
    expect(wrapper.classes()).toContain('type-primary')
  })

  it('renders dot element', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'OK' } })
    expect(wrapper.find('.dot').exists()).toBe(true)
  })

  it('dot does not have is-pulse class by default', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'OK' } })
    expect(wrapper.find('.dot').classes()).not.toContain('is-pulse')
  })

  it('dot has is-pulse class when pulse is true', () => {
    const wrapper = mount(StatusBadge, { props: { label: 'OK', pulse: true } })
    expect(wrapper.find('.dot').classes()).toContain('is-pulse')
  })
})
