import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ScoreTag from '@/components/ui/ScoreTag.vue'

describe('ScoreTag', () => {
  it('renders percentage by default', () => {
    const wrapper = mount(ScoreTag, { props: { score: 0.85 } })
    expect(wrapper.text()).toBe('85%')
  })

  it('renders 0% for score of 0', () => {
    const wrapper = mount(ScoreTag, { props: { score: 0 } })
    expect(wrapper.text()).toBe('0%')
  })

  it('renders 100% for score of 1', () => {
    const wrapper = mount(ScoreTag, { props: { score: 1 } })
    expect(wrapper.text()).toBe('100%')
  })

  it('renders decimal when showPercent is false', () => {
    const wrapper = mount(ScoreTag, {
      props: { score: 0.85, showPercent: false },
    })
    expect(wrapper.text()).toBe('0.85')
  })

  it('applies tone-high class for score > 0.8', () => {
    const wrapper = mount(ScoreTag, { props: { score: 0.9 } })
    expect(wrapper.classes()).toContain('tone-high')
  })

  it('applies tone-mid class for score >= 0.6 and <= 0.8', () => {
    const wrapper = mount(ScoreTag, { props: { score: 0.7 } })
    expect(wrapper.classes()).toContain('tone-mid')
  })

  it('applies tone-mid class for score exactly 0.6', () => {
    const wrapper = mount(ScoreTag, { props: { score: 0.6 } })
    expect(wrapper.classes()).toContain('tone-mid')
  })

  it('applies tone-low class for score < 0.6', () => {
    const wrapper = mount(ScoreTag, { props: { score: 0.3 } })
    expect(wrapper.classes()).toContain('tone-low')
  })

  it('applies tone-high class for score exactly 0.8 boundary', () => {
    // 0.8 is NOT > 0.8, so should be mid
    const wrapper = mount(ScoreTag, { props: { score: 0.8 } })
    expect(wrapper.classes()).toContain('tone-mid')
  })

  it('renders 0.50 when showPercent is false and score is 0.5', () => {
    const wrapper = mount(ScoreTag, {
      props: { score: 0.5, showPercent: false },
    })
    expect(wrapper.text()).toBe('0.50')
  })
})
