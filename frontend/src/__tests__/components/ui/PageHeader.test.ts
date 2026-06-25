import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PageHeader from '@/components/ui/PageHeader.vue'

const stubs = {
  'el-button': { template: '<button class="el-btn-stub"><slot /></button>' },
  'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
}

describe('PageHeader', () => {
  it('renders title', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'Dashboard' },
      stubs,
    })
    expect(wrapper.find('.title').text()).toBe('Dashboard')
  })

  it('does not render subtitle when not provided', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'T' },
      stubs,
    })
    expect(wrapper.find('.subtitle').exists()).toBe(false)
  })

  it('renders subtitle when provided', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'T', subtitle: 'Overview page' },
      stubs,
    })
    expect(wrapper.find('.subtitle').text()).toBe('Overview page')
  })

  it('does not render decoration by default', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'T' },
      stubs,
    })
    expect(wrapper.find('.decoration').exists()).toBe(false)
  })

  it('renders decoration when decoration is true', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'T', decoration: true },
      stubs,
    })
    expect(wrapper.find('.decoration').exists()).toBe(true)
    expect(wrapper.findAll('.d-dot')).toHaveLength(2)
    expect(wrapper.find('.d-line').exists()).toBe(true)
  })

  it('does not render back button by default', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'T' },
      stubs,
    })
    expect(wrapper.find('.back-btn').exists()).toBe(false)
  })

  it('renders back button when back is true', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'T', back: true },
      stubs,
    })
    expect(wrapper.find('.back-btn').exists()).toBe(true)
  })

  it('emits back event when back button is clicked', async () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'T', back: true },
      stubs,
    })
    await wrapper.find('.back-btn').trigger('click')
    expect(wrapper.emitted('back')).toHaveLength(1)
  })

  it('renders actions slot content', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'T' },
      stubs,
      slots: {
        actions: '<button class="action-btn">Add</button>',
      },
    })
    expect(wrapper.find('.action-btn').text()).toBe('Add')
  })
})
