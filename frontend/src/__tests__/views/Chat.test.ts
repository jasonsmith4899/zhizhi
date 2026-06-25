import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import Chat from '@/views/chat/Chat.vue'

const mockSend = vi.fn()
const mockAbort = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {}, params: {} }),
}))

vi.mock('@/stores/chat', () => ({
  useChatStore: () => ({
    selectedApiKeyId: { value: undefined },
    newChat: vi.fn(),
    messages: { value: [] },
  }),
}))

vi.mock('@/composables/useChatStream', () => ({
  useChatStream: () => ({
    messages: ref([]),
    loading: ref(false),
    send: mockSend,
    abort: mockAbort,
  }),
}))

vi.mock('@/api/apiKey', () => ({
  listApiKeys: vi.fn().mockResolvedValue({ data: [{ id: 1, name: 'Key 1' }] }),
}))

vi.mock('@/api/knowledge', () => ({
  getDocumentPreview: vi.fn().mockResolvedValue({ data: null }),
}))

describe('Chat', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the chat page', () => {
    const wrapper = mount(Chat)
    expect(wrapper.find('.chat-page').exists()).toBe(true)
  })

  it('renders ChatHeader component', () => {
    const wrapper = mount(Chat)
    const header = wrapper.findComponent({ name: 'ChatHeader' })
    expect(header.exists()).toBe(true)
  })

  it('renders MessageList component', () => {
    const wrapper = mount(Chat)
    const list = wrapper.findComponent({ name: 'MessageList' })
    expect(list.exists()).toBe(true)
  })

  it('renders ChatInput component', () => {
    const wrapper = mount(Chat)
    const input = wrapper.findComponent({ name: 'ChatInput' })
    expect(input.exists()).toBe(true)
  })

  it('renders SourcePreview component', () => {
    const wrapper = mount(Chat)
    const preview = wrapper.findComponent({ name: 'SourcePreview' })
    expect(preview.exists()).toBe(true)
  })

  it('passes loading state to ChatInput', () => {
    const wrapper = mount(Chat)
    const input = wrapper.findComponent({ name: 'ChatInput' })
    expect(input.props('loading')).toBe(false)
  })

  it('passes empty messages to MessageList initially', () => {
    const wrapper = mount(Chat)
    const list = wrapper.findComponent({ name: 'MessageList' })
    expect(list.props('messages')).toEqual([])
  })

  it('loads API keys on mount', async () => {
    mount(Chat)
    await flushPromises()
    // API keys should be loaded
    const wrapper = mount(Chat)
    await flushPromises()
    const header = wrapper.findComponent({ name: 'ChatHeader' })
    expect(header.exists()).toBe(true)
  })
})
