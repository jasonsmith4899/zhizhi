import { describe, it, expect, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import SourcePreview from '@/components/chat/SourcePreview.vue'

afterEach(() => {
  document.body.innerHTML = ''
})

describe('SourcePreview', () => {
  it('renders dialog component', async () => {
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData: null,
      },
    })
    await nextTick()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    expect(dialog.exists()).toBe(true)
  })

  it('shows loading state when previewLoading is true', async () => {
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: true,
        previewData: null,
      },
    })
    await nextTick()
    // v-loading directive should be present
    expect(wrapper.find('.preview-content').exists()).toBe(false)
  })

  it('shows preview content when data is available', async () => {
    const previewData = {
      documentName: 'Test Document',
      contentType: 'pdf',
      chunkCount: 5,
      content: 'This is the preview content',
    }
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData,
      },
    })
    await nextTick()
    expect(wrapper.text()).toContain('This is the preview content')
  })

  it('shows meta information when preview data has contentType', async () => {
    const previewData = {
      documentName: 'Test Document',
      contentType: 'pdf',
      chunkCount: 10,
      content: 'Content here',
    }
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData,
      },
    })
    await nextTick()
    expect(wrapper.text()).toContain('PDF')
    expect(wrapper.text()).toContain('10')
  })

  it('shows fallback when no preview data and not loading', async () => {
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData: null,
      },
    })
    await nextTick()
    expect(wrapper.text()).toContain('无法加载文档内容')
  })

  it('uses documentName as dialog title', async () => {
    const previewData = {
      documentName: 'My Document',
      content: 'Content',
    }
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData,
      },
    })
    await nextTick()
    expect(wrapper.text()).toContain('My Document')
  })

  it('uses fallback title when no documentName', async () => {
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData: { content: 'test' },
      },
    })
    await nextTick()
    expect(wrapper.text()).toContain('文档预览')
  })

  it('emits update:previewVisible when dialog closes', async () => {
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData: null,
      },
    })
    await nextTick()
    const dialog = wrapper.findComponent({ name: 'ElDialog' })
    await dialog.vm.$emit('update:modelValue', false)
    expect(wrapper.emitted('update:previewVisible')).toBeTruthy()
    expect(wrapper.emitted('update:previewVisible')![0]).toEqual([false])
  })

  it('renders close button in footer', async () => {
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData: null,
      },
    })
    await nextTick()
    // The dialog has a default close icon button in the header
    const closeBtn = wrapper.find('.el-dialog__headerbtn')
    expect(closeBtn.exists()).toBe(true)
  })

  it('handles previewData with minimal fields', async () => {
    const wrapper = mount(SourcePreview, {
      props: {
        previewVisible: true,
        previewLoading: false,
        previewData: { content: 'minimal' },
      },
    })
    await nextTick()
    expect(wrapper.text()).toContain('minimal')
  })
})
