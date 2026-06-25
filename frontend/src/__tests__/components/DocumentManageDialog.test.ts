import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import DocumentManageDialog from '@/components/knowledge/DocumentManageDialog.vue'

const mockGetCategories = vi.fn()
const mockCreateCategory = vi.fn()
const mockSetDocumentCategory = vi.fn()
const mockGetTags = vi.fn()
const mockCreateTag = vi.fn()
const mockGetDocumentTags = vi.fn()
const mockSetDocumentTags = vi.fn()
const mockGetDocumentVersions = vi.fn()
const mockRollbackDocumentVersion = vi.fn()

vi.mock('@/api/knowledge', () => ({
  getCategories: (...args: any[]) => mockGetCategories(...args),
  createCategory: (...args: any[]) => mockCreateCategory(...args),
  setDocumentCategory: (...args: any[]) => mockSetDocumentCategory(...args),
  getTags: (...args: any[]) => mockGetTags(...args),
  createTag: (...args: any[]) => mockCreateTag(...args),
  getDocumentTags: (...args: any[]) => mockGetDocumentTags(...args),
  setDocumentTags: (...args: any[]) => mockSetDocumentTags(...args),
  getDocumentVersions: (...args: any[]) => mockGetDocumentVersions(...args),
  rollbackDocumentVersion: (...args: any[]) => mockRollbackDocumentVersion(...args),
}))

describe('DocumentManageDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetCategories.mockResolvedValue({ data: [{ id: 1, name: 'Category A' }] })
    mockGetTags.mockResolvedValue({ data: [{ id: 1, name: 'Tag A' }] })
    mockGetDocumentTags.mockResolvedValue({ data: [1] })
    mockGetDocumentVersions.mockResolvedValue({ data: [] })
    mockCreateCategory.mockResolvedValue({})
    mockCreateTag.mockResolvedValue({})
    mockSetDocumentCategory.mockResolvedValue({})
    mockSetDocumentTags.mockResolvedValue({})
  })

  it('renders dialog when modelValue is true', async () => {
    const wrapper = mount(DocumentManageDialog, {
      props: {
        modelValue: true,
        documentId: 1,
        knowledgeBaseId: 1,
      },
    })
    await flushPromises()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    expect(dialog.exists()).toBe(true)
  })

  it('loads data when dialog opens', async () => {
    const wrapper = mount(DocumentManageDialog, {
      props: {
        modelValue: false,
        documentId: 1,
        knowledgeBaseId: 1,
      },
    })
    await flushPromises()
    // Watch triggers only when modelValue changes, not on initial mount
    await wrapper.setProps({ modelValue: true })
    await flushPromises()
    expect(mockGetCategories).toHaveBeenCalledWith(1)
    expect(mockGetTags).toHaveBeenCalledWith(1)
    expect(mockGetDocumentTags).toHaveBeenCalledWith(1)
    expect(mockGetDocumentVersions).toHaveBeenCalledWith(1)
  })

  it('renders tabs for category, tags, and versions', async () => {
    const wrapper = mount(DocumentManageDialog, {
      props: {
        modelValue: true,
        documentId: 1,
        knowledgeBaseId: 1,
      },
    })
    await flushPromises()
    const tabs = wrapper.findComponent({ name: 'ElTabs' })
    expect(tabs.exists()).toBe(true)
  })

  it('renders category tab with select', async () => {
    const wrapper = mount(DocumentManageDialog, {
      props: {
        modelValue: true,
        documentId: 1,
        knowledgeBaseId: 1,
      },
    })
    await flushPromises()
    // Category tab should be active by default
    expect(wrapper.text()).toContain('保存分类')
  })

  it('renders close button in footer', async () => {
    const wrapper = mount(DocumentManageDialog, {
      props: {
        modelValue: true,
        documentId: 1,
        knowledgeBaseId: 1,
      },
    })
    await flushPromises()
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const closeBtn = buttons.find(b => b.text().includes('关闭'))
    expect(closeBtn).toBeTruthy()
  })

  it('emits update:modelValue when dialog closes', async () => {
    const wrapper = mount(DocumentManageDialog, {
      props: {
        modelValue: true,
        documentId: 1,
        knowledgeBaseId: 1,
      },
    })
    await flushPromises()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    await dialog.vm.$emit('update:modelValue', false)
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })

  it('shows empty versions state when no versions', async () => {
    mockGetDocumentVersions.mockResolvedValue({ data: [] })
    const wrapper = mount(DocumentManageDialog, {
      props: {
        modelValue: true,
        documentId: 1,
        knowledgeBaseId: 1,
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('暂无版本记录')
  })
})
