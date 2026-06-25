import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Profile from '@/views/settings/Profile.vue'

const mockFetchUser = vi.fn()
const mockLogout = vi.fn()
const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ query: {}, params: {} }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    user: { id: 1, username: 'testuser', email: 'test@example.com', plan: 'free' },
    fetchUser: mockFetchUser,
    logout: mockLogout,
  }),
}))

const mockListApiKeys = vi.fn()
const mockCreateApiKey = vi.fn()
const mockUpdateApiKey = vi.fn()
const mockDeleteApiKey = vi.fn()

vi.mock('@/api/apiKey', () => ({
  listApiKeys: (...args: any[]) => mockListApiKeys(...args),
  createApiKey: (...args: any[]) => mockCreateApiKey(...args),
  updateApiKey: (...args: any[]) => mockUpdateApiKey(...args),
  deleteApiKey: (...args: any[]) => mockDeleteApiKey(...args),
}))

vi.mock('@/api/knowledge', () => ({
  getKnowledgeBases: vi.fn().mockResolvedValue({ data: [{ id: 1, name: 'KB 1' }] }),
}))

describe('Profile', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockListApiKeys.mockResolvedValue({
      data: [
        { id: 1, name: 'Production Key', keyValue: 'sk-abc...xyz', knowledgeBaseIds: [1], description: 'Prod key', createdAt: '2024-01-01T00:00:00Z' },
      ],
    })
  })

  it('renders the page container', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.find('.page-container').exists()).toBe(true)
  })

  it('renders page header', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('个人设置')
  })

  it('renders user info card', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('基本信息')
  })

  it('displays username', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.find('.user-name').text()).toBe('testuser')
  })

  it('displays user email', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.find('.user-email').text()).toBe('test@example.com')
  })

  it('displays user ID', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.find('.user-id').text()).toContain('1')
  })

  it('displays user avatar initial', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.find('.user-avatar span').text()).toBe('t')
  })

  it('renders plan info card', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('套餐信息')
  })

  it('displays plan name', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('免费版')
  })

  it('renders API key management section', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('API Key 管理')
  })

  it('renders create API key button', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('新建 Key')
  })

  it('loads API keys on mount', async () => {
    mount(Profile)
    await flushPromises()
    expect(mockListApiKeys).toHaveBeenCalled()
  })

  it('renders API key table', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    const table = wrapper.findComponent({ name: 'ElTable' })
    expect(table.exists()).toBe(true)
  })

  it('renders API key data', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('Production Key')
  })

  it('renders API key info section', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('X-API-Key')
  })

  it('renders danger zone card', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('账户操作')
  })

  it('renders logout button', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const logoutBtn = buttons.find(b => b.text().includes('退出登录'))
    expect(logoutBtn).toBeTruthy()
  })

  it('calls logout and navigates to login on logout click', async () => {
    const wrapper = mount(Profile, {
      global: {
        mocks: {
          $router: { push: mockPush },
        },
      },
    })
    await flushPromises()
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const logoutBtn = buttons.find(b => b.text().includes('退出登录'))
    if (logoutBtn) {
      await logoutBtn.trigger('click')
      expect(mockLogout).toHaveBeenCalled()
      expect(mockPush).toHaveBeenCalledWith('/login')
    }
  })

  it('renders edit and delete buttons for API keys', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('编辑')
    expect(wrapper.text()).toContain('删除')
  })

  it('renders API dialog', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    expect(dialog.exists()).toBe(true)
  })

  it('shows empty state when no API keys', async () => {
    mockListApiKeys.mockResolvedValue({ data: [] })
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('尚未创建 API Key')
  })

  it('renders API description info', async () => {
    const wrapper = mount(Profile)
    await flushPromises()
    expect(wrapper.text()).toContain('API Key 用于通过 API 接口调用智能问答服务')
  })

  it('fetches user on mount', async () => {
    mount(Profile)
    await flushPromises()
    expect(mockFetchUser).toHaveBeenCalled()
  })
})
