import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import SectionCard from '@/components/ui/SectionCard.vue'

const GlassCardStub = {
  template: '<div class="glass-card-stub"><slot /></div>',
  props: ['padding', 'glowOnHover'],
}
const IconBadgeStub = {
  template: '<div class="icon-badge-stub"><slot /></div>',
  props: ['size', 'variant', 'radius'],
}
const DecorLineStub = {
  template: '<div class="decor-line-stub"></div>',
  props: ['variant'],
}

const globalOpts = {
  plugins: [ElementPlus],
  stubs: {
    GlassCard: GlassCardStub,
    IconBadge: IconBadgeStub,
    DecorLine: DecorLineStub,
  },
}

describe('SectionCard', () => {
  it('renders title', () => {
    const wrapper = mount(SectionCard, {
      props: { title: 'Settings' },
      global: globalOpts,
    })
    expect(wrapper.find('.section-title').text()).toBe('Settings')
  })

  it('renders default slot content', () => {
    const wrapper = mount(SectionCard, {
      props: { title: 'T' },
      global: globalOpts,
      slots: { default: '<p class="content">Body</p>' },
    })
    expect(wrapper.find('.content').text()).toBe('Body')
  })

  it('does not render IconBadge when no icon prop', () => {
    const wrapper = mount(SectionCard, {
      props: { title: 'T' },
      global: globalOpts,
    })
    expect(wrapper.find('.icon-badge-stub').exists()).toBe(false)
  })

  it('renders IconBadge when icon prop is provided', () => {
    const TestIcon = { template: '<span>icon</span>' }
    const wrapper = mount(SectionCard, {
      props: { title: 'T', icon: TestIcon },
      global: globalOpts,
    })
    expect(wrapper.find('.icon-badge-stub').exists()).toBe(true)
  })

  it('passes ghost-danger variant to IconBadge when danger is true', () => {
    const TestIcon = { template: '<span>icon</span>' }
    const wrapper = mount(SectionCard, {
      props: { title: 'T', icon: TestIcon, danger: true },
      global: globalOpts,
    })
    const badge = wrapper.findComponent(IconBadgeStub)
    expect(badge.props('variant')).toBe('ghost-danger')
  })

  it('passes ghost variant to IconBadge when danger is false', () => {
    const TestIcon = { template: '<span>icon</span>' }
    const wrapper = mount(SectionCard, {
      props: { title: 'T', icon: TestIcon, danger: false },
      global: globalOpts,
    })
    const badge = wrapper.findComponent(IconBadgeStub)
    expect(badge.props('variant')).toBe('ghost')
  })

  it('renders header-actions slot', () => {
    const wrapper = mount(SectionCard, {
      props: { title: 'T' },
      global: globalOpts,
      slots: {
        'header-actions': '<button class="header-btn">Edit</button>',
      },
    })
    expect(wrapper.find('.header-btn').text()).toBe('Edit')
  })

  it('renders DecorLine', () => {
    const wrapper = mount(SectionCard, {
      props: { title: 'T' },
      global: globalOpts,
    })
    expect(wrapper.find('.decor-line-stub').exists()).toBe(true)
  })
})
