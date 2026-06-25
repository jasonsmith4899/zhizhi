import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import DocumentPreview from '@/components/knowledge/DocumentPreview.vue'

const mockGetDocumentRaw = vi.fn()
const mockGetDocumentPreview = vi.fn()

vi.mock('@/api/knowledge', () => ({
  getDocumentRaw: (...args: any[]) => mockGetDocumentRaw(...args),
  getDocumentPreview: (...args: any[]) => mockGetDocumentPreview(...args),
}))

vi.mock('@/utils/markdown', () => ({
  renderMarkdown: (text: string) => `<p>${text}</p>`,
}))

describe('DocumentPreview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows empty state when no documentId', () => {
    const wrapper = mount(DocumentPreview, {
      props: {
        documentId: null,
        filename: 'test.txt',
        fileType: 'txt',
      },
    })
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
  })

  it('loads text content for txt files', async () => {
    const mockBlob = new Blob(['Hello World'], { type: 'text/plain' })
    mockGetDocumentRaw.mockResolvedValue(mockBlob)

    const wrapper = mount(DocumentPreview, {
      props: {
        documentId: 1,
        filename: 'test.txt',
        fileType: 'txt',
      },
    })
    await flushPromises()
    expect(mockGetDocumentRaw).toHaveBeenCalledWith(1)
    expect(wrapper.find('.preview-text').text()).toBe('Hello World')
  })

  it('loads and renders markdown content', async () => {
    const mockBlob = new Blob(['# Hello'], { type: 'text/markdown' })
    mockGetDocumentRaw.mockResolvedValue(mockBlob)

    const wrapper = mount(DocumentPreview, {
      props: {
        documentId: 1,
        filename: 'test.md',
        fileType: 'md',
      },
    })
    await flushPromises()
    expect(wrapper.find('.preview-content').exists()).toBe(true)
  })

  it('loads PDF content and creates iframe', async () => {
    const mockBlob = new Blob(['%PDF'], { type: 'application/pdf' })
    mockGetDocumentRaw.mockResolvedValue(mockBlob)

    const wrapper = mount(DocumentPreview, {
      props: {
        documentId: 1,
        filename: 'test.pdf',
        fileType: 'pdf',
      },
    })
    await flushPromises()
    expect(wrapper.find('.pdf-frame').exists()).toBe(true)
  })

  it('falls back to getDocumentPreview when getDocumentRaw fails', async () => {
    mockGetDocumentRaw.mockRejectedValue(new Error('No raw file'))
    mockGetDocumentPreview.mockResolvedValue({
      data: { content: 'Preview content from chunks' },
    })

    const wrapper = mount(DocumentPreview, {
      props: {
        documentId: 1,
        filename: 'test.txt',
        fileType: 'txt',
      },
    })
    await flushPromises()
    expect(mockGetDocumentRaw).toHaveBeenCalledWith(1)
    expect(mockGetDocumentPreview).toHaveBeenCalledWith(1)
    expect(wrapper.find('.preview-text').text()).toBe('Preview content from chunks')
  })

  it('shows error empty state when both APIs fail', async () => {
    mockGetDocumentRaw.mockRejectedValue(new Error('Network error'))
    mockGetDocumentPreview.mockRejectedValue(new Error('Not found'))

    const wrapper = mount(DocumentPreview, {
      props: {
        documentId: 1,
        filename: 'test.txt',
        fileType: 'txt',
      },
    })
    await flushPromises()
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
  })

  it('reloads when documentId changes', async () => {
    const mockBlob = new Blob(['Content'], { type: 'text/plain' })
    mockGetDocumentRaw.mockResolvedValue(mockBlob)

    const wrapper = mount(DocumentPreview, {
      props: {
        documentId: 1,
        filename: 'test.txt',
        fileType: 'txt',
      },
    })
    await flushPromises()
    expect(mockGetDocumentRaw).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ documentId: 2 })
    await flushPromises()
    expect(mockGetDocumentRaw).toHaveBeenCalledTimes(2)
  })

  it('handles empty fileType gracefully', async () => {
    const mockBlob = new Blob(['Content'], { type: 'text/plain' })
    mockGetDocumentRaw.mockResolvedValue(mockBlob)

    const wrapper = mount(DocumentPreview, {
      props: {
        documentId: 1,
        filename: 'test',
        fileType: '',
      },
    })
    await flushPromises()
    expect(wrapper.find('.preview-text').exists()).toBe(true)
  })
})
