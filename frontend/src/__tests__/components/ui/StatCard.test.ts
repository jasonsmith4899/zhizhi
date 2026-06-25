import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import StatCard from '@/components/ui/StatCard.vue'

const GlassCardStub = {
  template: '<div class="glass-card-stub"><slot /></div>',
  props: ['lift', 'topBar'],
}
const IconBadgeStub = {
  template: '<div class="icon-badge-stub"><slot /></div>',
  props: ['size', 'glow'],
}
const HexDecorationStub = {
  template: '<div class="hex-deco-stub"></div>',
  props: ['size', 'variant', 'opacity'],
}

const globalOpts = {
  plugins: [ElementPlus],
  stubs: {
    GlassCard: GlassCardStub,
    IconBadge: IconBadgeStub,
    HexDecoration: HexDecorationStub,
  },
}

describe('StatCard', () => {
  it('renders label', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'Total Users', value: 1234 },
      global: globalOpts,
    })
    expect(wrapper.find('.stat-label').text()).toBe('Total Users')
  })

  it('renders numeric value', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'Count', value: 42 },
      global: globalOpts,
    })
    expect(wrapper.find('.stat-value').text()).toBe('42')
  })

  it('renders string value', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'Status', value: 'Active' },
      global: globalOpts,
    })
    expect(wrapper.find('.stat-value').text()).toBe('Active')
  })

  it('does not render IconBadge when no icon prop', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'L', value: 1 },
      global: globalOpts,
    })
    expect(wrapper.find('.icon-badge-stub').exists()).toBe(false)
  })

  it('renders IconBadge when icon prop is provided', () => {
    const TestIcon = { template: '<span>icon</span>' }
    const wrapper = mount(StatCard, {
      props: { label: 'L', value: 1, icon: TestIcon },
      global: globalOpts,
    })
    expect(wrapper.find('.icon-badge-stub').exists()).toBe(true)
  })

  it('applies is-text class when isText is true', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'L', value: 'text', isText: true },
      global: globalOpts,
    })
    expect(wrapper.find('.stat-value').classes()).toContain('is-text')
  })

  it('does not apply is-text class by default', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'L', value: 1 },
      global: globalOpts,
    })
    expect(wrapper.find('.stat-value').classes()).not.toContain('is-text')
  })

  it('renders HexDecoration by default', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'L', value: 1 },
      global: globalOpts,
    })
    expect(wrapper.find('.hex-deco-stub').exists()).toBe(true)
  })

  it('does not render HexDecoration when hex is false', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'L', value: 1, hex: false },
      global: globalOpts,
    })
    expect(wrapper.find('.hex-deco-stub').exists()).toBe(false)
  })

  it('renders stat-body and stat-info containers', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'L', value: 1 },
      global: globalOpts,
    })
    expect(wrapper.find('.stat-body').exists()).toBe(true)
    expect(wrapper.find('.stat-info').exists()).toBe(true)
  })
})
