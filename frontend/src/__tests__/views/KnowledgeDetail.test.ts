import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import KnowledgeDetail from '@/views/knowledge/KnowledgeDetail.vue'

const mockPush = vi.fn()
const mockGetKnowledgeBase = vi.fn()
const mockGetDocuments = vi.fn()
const mockUploadDocument = vi.fn()
const mockDeleteDocument = vi.fn()
const mockGetVectorStatus = vi.fn()
const mockReVectorize = vi.fn()
const mockBatchDeleteDocuments = vi.fn()
const mockGetDocumentDownloadUrl = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({
    params: { id: '1' },
    query: {},
  }),
}))

vi.mock('@/api/knowledge', () => ({
  getKnowledgeBase: (...args: any[]) => mockGetKnowledgeBase(...args),
  getDocuments: (...args: any[]) => mockGetDocuments(...args),
  uploadDocument: (...args: any[]) => mockUploadDocument(...args),
  deleteDocument: (...args: any[]) => mockDeleteDocument(...args),
  getVectorStatus: (...args: any[]) => mockGetVectorStatus(...args),
  reVectorize: (...args: any[]) => mockReVectorize(...args),
  batchDeleteDocuments: (...args: any[]) => mockBatchDeleteDocuments(...args),
  getDocumentDownloadUrl: (...args: any[]) => mockGetDocumentDownloadUrl(...args),
  getDocumentPreview: vi.fn().mockResolvedValue({ data: null }),
  getDocumentRaw: vi.fn().mockResolvedValue(new Blob()),
  getCategories: vi.fn().mockResolvedValue({ data: [] }),
  getTags: vi.fn().mockResolvedValue({ data: [] }),
  getDocumentTags: vi.fn().mockResolvedValue({ data: [] }),
  getDocumentVersions: vi.fn().mockResolvedValue({ data: [] }),
  setDocumentCategory: vi.fn().mockResolvedValue({}),
  setDocumentTags: vi.fn().mockResolvedValue({}),
  createCategory: vi.fn().mockResolvedValue({}),
  createTag: vi.fn().mockResolvedValue({}),
  rollbackDocumentVersion: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/kg', () => ({
  getKgStats: vi.fn().mockResolvedValue({ data: { entityCount: 0, relationCount: 0, avgMentionCount: 0, topEntities: [], typeDistribution: [] } }),
  getKgEntities: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
  getKgEntityDetail: vi.fn().mockResolvedValue({ data: null }),
  getKgRelations: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
  getKgGraph: vi.fn().mockResolvedValue({ data: { nodes: [], edges: [] } }),
  searchKgEntities: vi.fn().mockResolvedValue({ data: [] }),
}))

vi.mock('@/components/ui/SectionCard.vue', () => ({
  default: { name: 'SectionCard', template: '<div class="section-card"><slot /></div>' },
}))
vi.mock('@/components/ui/StatCard.vue', () => ({
  default: { name: 'StatCard', template: '<div class="stat-card" />' },
}))
vi.mock('@/components/ui/GlassCard.vue', () => ({
  default: { name: 'GlassCard', template: '<div class="glass-card"><slot /></div>' },
}))
vi.mock('@/components/ui/EmptyState.vue', () => ({
  default: { name: 'EmptyState', template: '<div class="empty-state" />' },
}))
vi.mock('@/components/ui/ShineButton.vue', () => ({
  default: { name: 'ShineButton', template: '<button class="shine-btn"><slot /></button>' },
}))
vi.mock('@/components/ui/ScoreTag.vue', () => ({
  default: { name: 'ScoreTag', template: '<span class="score-tag" />' },
}))

vi.mock('vis-network/standalone', () => ({
  Network: vi.fn().mockImplementation(() => ({
    on: vi.fn(),
    destroy: vi.fn(),
    focus: vi.fn(),
    selectNodes: vi.fn(),
  })),
  DataSet: vi.fn().mockImplementation(() => ({
    getIds: vi.fn().mockReturnValue([]),
    update: vi.fn(),
  })),
}))

describe('KnowledgeDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetKnowledgeBase.mockResolvedValue({
      data: { id: 1, name: 'Test KB', description: 'A test KB', documentCount: 5, chunkCount: 50 },
    })
    mockGetDocuments.mockResolvedValue({
      data: [
        { id: 1, filename: 'doc1.pdf', fileType: 'pdf', status: 'ready', chunkCount: 10, createdAt: '2024-01-01T00:00:00Z' },
        { id: 2, filename: 'doc2.txt', fileType: 'txt', status: 'processing', chunkCount: 0, createdAt: '2024-01-02T00:00:00Z' },
      ],
    })
    mockGetVectorStatus.mockResolvedValue({ data: { status: 'ready', chunkCount: 10, vectorCount: 10 } })
  })

  it('renders the page container', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    expect(wrapper.find('.page-container').exists()).toBe(true)
  })

  it('renders back button', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    const backBtn = wrapper.findComponent({ name: 'ElButton' })
    expect(backBtn.exists()).toBe(true)
  })

  it('loads knowledge base on mount', async () => {
    mount(KnowledgeDetail)
    await flushPromises()
    expect(mockGetKnowledgeBase).toHaveBeenCalledWith(1)
  })

  it('loads documents on mount', async () => {
    mount(KnowledgeDetail)
    await flushPromises()
    expect(mockGetDocuments).toHaveBeenCalledWith(1)
  })

  it('renders KB info card', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    expect(wrapper.text()).toContain('Test KB')
  })

  it('renders tabs', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    const tabs = wrapper.findComponent({ name: 'ElTabs' })
    expect(tabs.exists()).toBe(true)
  })

  it('renders document tab', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    expect(wrapper.text()).toContain('文档管理')
  })

  it('renders knowledge graph tab', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    expect(wrapper.text()).toContain('知识图谱')
  })

  it('renders upload button', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    expect(wrapper.text()).toContain('上传文档')
  })

  it('renders document table', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    const table = wrapper.findComponent({ name: 'ElTable' })
    expect(table.exists()).toBe(true)
  })

  it('renders document filenames', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    expect(wrapper.text()).toContain('doc1.pdf')
    expect(wrapper.text()).toContain('doc2.txt')
  })

  it('renders preview and manage buttons for documents', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    expect(wrapper.text()).toContain('预览')
    expect(wrapper.text()).toContain('管理')
    expect(wrapper.text()).toContain('删除')
  })

  it('navigates back when back button is clicked', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()

    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const backBtn = buttons[0]
    await backBtn.trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/knowledge')
  })

  it('shows empty state when no documents', async () => {
    mockGetDocuments.mockResolvedValue({ data: [] })
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    expect(wrapper.text()).toContain('暂无文档')
  })

  it('renders document preview dialog', async () => {
    const wrapper = mount(KnowledgeDetail)
    await flushPromises()
    // There should be at least one dialog
    const dialogs = wrapper.findAllComponents({ name: 'ElDialog' })
    expect(dialogs.length).toBeGreaterThanOrEqual(1)
  })
})
