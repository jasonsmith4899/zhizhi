import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ShineButton from '@/components/ui/ShineButton.vue'

const globalOpts = {
  plugins: [ElementPlus],
}

describe('ShineButton', () => {
  it('renders slot content', () => {
    const wrapper = mount(ShineButton, {
      global: globalOpts,
      slots: { default: 'Submit' },
    })
    expect(wrapper.text()).toContain('Submit')
  })

  it('applies default size-lg class', () => {
    const wrapper = mount(ShineButton, { global: globalOpts })
    expect(wrapper.classes()).toContain('size-lg')
  })

  it('applies size-md class', () => {
    const wrapper = mount(ShineButton, { global: globalOpts, props: { size: 'md' } })
    expect(wrapper.classes()).toContain('size-md')
  })

  it('applies size-xl class', () => {
    const wrapper = mount(ShineButton, { global: globalOpts, props: { size: 'xl' } })
    expect(wrapper.classes()).toContain('size-xl')
  })

  it('applies is-block class when block is true', () => {
    const wrapper = mount(ShineButton, { global: globalOpts, props: { block: true } })
    expect(wrapper.classes()).toContain('is-block')
  })

  it('does not apply is-block by default', () => {
    const wrapper = mount(ShineButton, { global: globalOpts })
    expect(wrapper.classes()).not.toContain('is-block')
  })

  it('emits click event on button click', async () => {
    const wrapper = mount(ShineButton, { global: globalOpts })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mount(ShineButton, { global: globalOpts, props: { disabled: true } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('sets disabled attribute when disabled', () => {
    const wrapper = mount(ShineButton, { global: globalOpts, props: { disabled: true } })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('sets disabled attribute when loading', () => {
    const wrapper = mount(ShineButton, { global: globalOpts, props: { loading: true } })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('does not emit click when loading', async () => {
    const wrapper = mount(ShineButton, { global: globalOpts, props: { loading: true } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('renders loading spinner when loading is true', () => {
    const wrapper = mount(ShineButton, { global: globalOpts, props: { loading: true } })
    expect(wrapper.find('.spin').exists()).toBe(true)
  })

  it('does not render loading spinner by default', () => {
    const wrapper = mount(ShineButton, { global: globalOpts })
    expect(wrapper.find('.spin').exists()).toBe(false)
  })

  it('renders icon when icon prop is provided and not loading', () => {
    const TestIcon = { template: '<span>icon</span>' }
    const wrapper = mount(ShineButton, {
      global: globalOpts,
      props: { icon: TestIcon },
    })
    expect(wrapper.find('.el-icon').exists()).toBe(true)
  })

  it('renders shine effect element', () => {
    const wrapper = mount(ShineButton, { global: globalOpts })
    expect(wrapper.find('.shine').exists()).toBe(true)
  })

  it('renders content wrapper', () => {
    const wrapper = mount(ShineButton, { global: globalOpts })
    expect(wrapper.find('.content').exists()).toBe(true)
  })
})
