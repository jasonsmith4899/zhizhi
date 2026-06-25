import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import EmptyState from '@/components/ui/EmptyState.vue'

const HexStub = { template: '<div class="hex-stub"><slot /></div>', props: ['size', 'variant'] }

const globalOpts = {
  plugins: [ElementPlus],
  stubs: {
    HexDecoration: HexStub,
  },
}

describe('EmptyState', () => {
  it('renders title', () => {
    const wrapper = mount(EmptyState, {
      props: { title: 'No Data' },
      global: globalOpts,
    })
    expect(wrapper.find('.empty-title').text()).toBe('No Data')
  })

  it('applies default size-lg class', () => {
    const wrapper = mount(EmptyState, { props: { title: 'T' }, global: globalOpts })
    expect(wrapper.classes()).toContain('size-lg')
  })

  it('applies size-md class when size is md', () => {
    const wrapper = mount(EmptyState, { props: { title: 'T', size: 'md' }, global: globalOpts })
    expect(wrapper.classes()).toContain('size-md')
  })

  it('does not render subtitle when not provided', () => {
    const wrapper = mount(EmptyState, { props: { title: 'T' }, global: globalOpts })
    expect(wrapper.find('.empty-subtitle').exists()).toBe(false)
  })

  it('renders subtitle when provided', () => {
    const wrapper = mount(EmptyState, {
      props: { title: 'T', subtitle: 'Please add items' },
      global: globalOpts,
    })
    expect(wrapper.find('.empty-subtitle').text()).toBe('Please add items')
  })

  it('renders HexDecoration stub when icon prop is provided', () => {
    const TestIcon = { template: '<span class="test-icon">icon</span>' }
    const wrapper = mount(EmptyState, {
      props: { title: 'T', icon: TestIcon },
      global: globalOpts,
    })
    expect(wrapper.find('.hex-stub').exists()).toBe(true)
  })

  it('renders text inside hex-text when text prop is provided', () => {
    const wrapper = mount(EmptyState, {
      props: { title: 'T', text: '?' },
      global: globalOpts,
    })
    expect(wrapper.find('.hex-text').text()).toBe('?')
  })

  it('does not render decoration by default', () => {
    const wrapper = mount(EmptyState, { props: { title: 'T' }, global: globalOpts })
    expect(wrapper.find('.empty-decoration').exists()).toBe(false)
  })

  it('renders decoration when decoration is true', () => {
    const wrapper = mount(EmptyState, {
      props: { title: 'T', decoration: true },
      global: globalOpts,
    })
    expect(wrapper.find('.empty-decoration').exists()).toBe(true)
    expect(wrapper.findAll('.dl')).toHaveLength(3)
  })

  it('renders actions slot content', () => {
    const wrapper = mount(EmptyState, {
      props: { title: 'T' },
      global: globalOpts,
      slots: {
        actions: '<button class="action-btn">Add</button>',
      },
    })
    expect(wrapper.find('.action-btn').text()).toBe('Add')
  })
})
