import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import Layout from '@/views/Layout.vue'

const mockPush = vi.fn()
const mockFetchUser = vi.fn()
const mockLogout = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ path: '/dashboard', meta: { title: '仪表盘' } }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    user: { username: 'testuser' },
    fetchUser: mockFetchUser,
    logout: mockLogout,
  }),
}))

describe('Layout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the layout container', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.layout-container').exists()).toBe(true)
  })

  it('renders sidebar', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.sidebar').exists()).toBe(true)
  })

  it('renders logo', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.logo-char').text()).toBe('智')
    expect(wrapper.find('.logo-title').text()).toBe('智知')
  })

  it('renders logo subtitle', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.logo-subtitle').text()).toBe('AI Knowledge')
  })

  it('renders navigation menu', () => {
    const wrapper = mount(Layout)
    const menu = wrapper.findComponent({ name: 'ElMenu' })
    expect(menu.exists()).toBe(true)
  })

  it('renders all menu items', () => {
    const wrapper = mount(Layout)
    const menuItems = wrapper.findAllComponents({ name: 'ElMenuItem' })
    expect(menuItems).toHaveLength(6)
  })

  it('renders menu item titles', () => {
    const wrapper = mount(Layout)
    const titles = wrapper.findAll('.menu-title')
    const texts = titles.map(t => t.text())
    expect(texts).toContain('仪表盘')
    expect(texts).toContain('知识库')
    expect(texts).toContain('AI 对话')
    expect(texts).toContain('对话记录')
    expect(texts).toContain('个人设置')
    expect(texts).toContain('操作审计')
  })

  it('renders top header', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.top-header').exists()).toBe(true)
  })

  it('renders breadcrumb', () => {
    const wrapper = mount(Layout)
    const breadcrumb = wrapper.findComponent({ name: 'ElBreadcrumb' })
    expect(breadcrumb.exists()).toBe(true)
  })

  it('renders user avatar', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.user-avatar').exists()).toBe(true)
  })

  it('renders user name', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.user-name').text()).toBe('testuser')
  })

  it('renders collapse button', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.collapse-btn').exists()).toBe(true)
  })

  it('toggles sidebar collapse on button click', async () => {
    const wrapper = mount(Layout)
    expect(wrapper.vm.isCollapsed).toBe(false)

    await wrapper.find('.collapse-btn').trigger('click')
    expect(wrapper.vm.isCollapsed).toBe(true)

    await wrapper.find('.collapse-btn').trigger('click')
    expect(wrapper.vm.isCollapsed).toBe(false)
  })

  it('renders version text when not collapsed', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.version-text').text()).toBe('v1.0.0')
  })

  it('renders main content area', () => {
    const wrapper = mount(Layout)
    expect(wrapper.find('.main-content').exists()).toBe(true)
  })

  it('renders router-view', () => {
    const wrapper = mount(Layout, {
      global: {
        stubs: {
          RouterView: { template: '<div class="router-view-stub" />' },
        },
      },
    })
    expect(wrapper.find('.router-view-stub').exists()).toBe(true)
  })

  it('fetches user on mount', () => {
    mount(Layout)
    expect(mockFetchUser).toHaveBeenCalled()
  })
})
