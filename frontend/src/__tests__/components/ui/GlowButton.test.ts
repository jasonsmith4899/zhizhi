import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import GlowButton from '@/components/ui/GlowButton.vue'

const globalOpts = {
  plugins: [ElementPlus],
}

describe('GlowButton', () => {
  it('renders slot content', () => {
    const wrapper = mount(GlowButton, {
      global: globalOpts,
      slots: { default: 'Click Me' },
    })
    expect(wrapper.text()).toContain('Click Me')
  })

  it('applies default classes: variant-secondary, size-md', () => {
    const wrapper = mount(GlowButton, { global: globalOpts })
    expect(wrapper.classes()).toContain('variant-secondary')
    expect(wrapper.classes()).toContain('size-md')
  })

  it('applies variant-danger class', () => {
    const wrapper = mount(GlowButton, {
      global: globalOpts,
      props: { variant: 'danger' },
    })
    expect(wrapper.classes()).toContain('variant-danger')
  })

  it('applies size-sm class', () => {
    const wrapper = mount(GlowButton, { global: globalOpts, props: { size: 'sm' } })
    expect(wrapper.classes()).toContain('size-sm')
  })

  it('applies size-lg class', () => {
    const wrapper = mount(GlowButton, { global: globalOpts, props: { size: 'lg' } })
    expect(wrapper.classes()).toContain('size-lg')
  })

  it('applies size-xl class', () => {
    const wrapper = mount(GlowButton, { global: globalOpts, props: { size: 'xl' } })
    expect(wrapper.classes()).toContain('size-xl')
  })

  it('emits click event on button click', async () => {
    const wrapper = mount(GlowButton, { global: globalOpts })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('passes MouseEvent in click emission', async () => {
    const wrapper = mount(GlowButton, { global: globalOpts })
    await wrapper.find('button').trigger('click')
    const emitted = wrapper.emitted('click')
    expect(emitted).toBeDefined()
    expect(emitted![0]).toHaveLength(1)
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mount(GlowButton, { global: globalOpts, props: { disabled: true } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('sets disabled attribute on button when disabled', () => {
    const wrapper = mount(GlowButton, { global: globalOpts, props: { disabled: true } })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('applies is-active class when active is true', () => {
    const wrapper = mount(GlowButton, { global: globalOpts, props: { active: true } })
    expect(wrapper.classes()).toContain('is-active')
  })

  it('does not apply is-active class by default', () => {
    const wrapper = mount(GlowButton, { global: globalOpts })
    expect(wrapper.classes()).not.toContain('is-active')
  })

  it('renders icon when icon prop is provided', () => {
    const TestIcon = { template: '<span>icon</span>' }
    const wrapper = mount(GlowButton, {
      global: globalOpts,
      props: { icon: TestIcon },
    })
    // el-icon should be rendered
    expect(wrapper.find('.el-icon').exists()).toBe(true)
  })
})
