import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Login from '@/views/Login.vue'

const mockPush = vi.fn()
const mockLogin = vi.fn()
const mockRegister = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ query: {} }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    login: mockLogin,
    register: mockRegister,
  }),
}))

describe('Login', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login form', () => {
    const wrapper = mount(Login)
    expect(wrapper.find('.login-form').exists()).toBe(true)
  })

  it('renders page title for login mode', () => {
    const wrapper = mount(Login)
    expect(wrapper.find('.form-header h2').text()).toBe('欢迎回来')
  })

  it('renders username input', () => {
    const wrapper = mount(Login)
    expect(wrapper.text()).toContain('用户名')
  })

  it('renders password input', () => {
    const wrapper = mount(Login)
    expect(wrapper.text()).toContain('密码')
  })

  it('does not show email input in login mode', () => {
    const wrapper = mount(Login)
    expect(wrapper.text()).not.toContain('邮箱')
  })

  it('shows email input in register mode', async () => {
    const wrapper = mount(Login)
    // Click the toggle link to switch to register
    const link = wrapper.findComponent({ name: 'ElLink' })
    await link.trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('邮箱')
  })

  it('shows register title in register mode', async () => {
    const wrapper = mount(Login)
    const link = wrapper.findComponent({ name: 'ElLink' })
    await link.trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.form-header h2').text()).toBe('创建账户')
  })

  it('renders submit button with login text', () => {
    const wrapper = mount(Login)
    expect(wrapper.find('.submit-btn').text()).toContain('登录')
  })

  it('shows register text on submit button in register mode', async () => {
    const wrapper = mount(Login)
    const link = wrapper.findComponent({ name: 'ElLink' })
    await link.trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.submit-btn').text()).toContain('注册')
  })

  it('renders toggle link', () => {
    const wrapper = mount(Login)
    expect(wrapper.text()).toContain('没有账户？去注册')
  })

  it('shows correct toggle link in register mode', async () => {
    const wrapper = mount(Login)
    const link = wrapper.findComponent({ name: 'ElLink' })
    await link.trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('已有账户？去登录')
  })

  it('renders logo text', () => {
    const wrapper = mount(Login)
    expect(wrapper.find('.logo-text').text()).toBe('智知')
  })

  it('renders logo subtitle', () => {
    const wrapper = mount(Login)
    expect(wrapper.find('.logo-subtitle').text()).toBe('ZhiZhi AI')
  })

  it('renders tagline', () => {
    const wrapper = mount(Login)
    expect(wrapper.text()).toContain('智能知识库')
    expect(wrapper.text()).toContain('未来已来')
  })

  it('renders form with el-form component', () => {
    const wrapper = mount(Login)
    const form = wrapper.findComponent({ name: 'ElForm' })
    expect(form.exists()).toBe(true)
  })

  it('has password field with show-password', () => {
    const wrapper = mount(Login)
    const inputs = wrapper.findAllComponents({ name: 'ElInput' })
    const passwordInput = inputs.find(i => i.props('type') === 'password')
    expect(passwordInput).toBeTruthy()
    expect(passwordInput!.props('showPassword')).toBe(true)
  })
})
