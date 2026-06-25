import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  getKnowledgeBases,
  getKnowledgeBase,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase,
  getDocuments,
  uploadDocument,
  deleteDocument,
  getDocumentChunks,
  getVectorStatus,
  reVectorize,
  batchDeleteDocuments,
  getDocumentPreview,
  getDocumentDownloadUrl,
  getDocumentRaw,
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  getTags,
  createTag,
  deleteTag,
  setDocumentCategory,
  setDocumentTags,
  getDocumentTags,
  getDocumentVersions,
  rollbackDocumentVersion,
} from '@/api/knowledge'

vi.mock('@/api/request', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    },
  }
})

import request from '@/api/request'
const mockedRequest = vi.mocked(request)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/knowledge', () => {
  describe('knowledge base CRUD', () => {
    it('getKnowledgeBases should GET /knowledge-bases', async () => {
      const res = { code: 200, data: [] }
      mockedRequest.get.mockResolvedValue(res)
      const result = await getKnowledgeBases()
      expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases')
      expect(result).toBe(res)
    })

    it('getKnowledgeBase should GET /knowledge-bases/:id', async () => {
      const res = { code: 200, data: { id: 1, name: 'test' } }
      mockedRequest.get.mockResolvedValue(res)
      const result = await getKnowledgeBase(1)
      expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1')
      expect(result).toBe(res)
    })

    it('createKnowledgeBase should POST /knowledge-bases', async () => {
      const res = { code: 200, data: { id: 2 } }
      mockedRequest.post.mockResolvedValue(res)
      const result = await createKnowledgeBase({ name: 'new', description: 'desc' })
      expect(mockedRequest.post).toHaveBeenCalledWith('/knowledge-bases', {
        name: 'new',
        description: 'desc',
      })
      expect(result).toBe(res)
    })

    it('updateKnowledgeBase should PUT /knowledge-bases/:id', async () => {
      const res = { code: 200, data: { id: 1 } }
      mockedRequest.put.mockResolvedValue(res)
      const result = await updateKnowledgeBase(1, { name: 'updated', systemPrompt: 'prompt' })
      expect(mockedRequest.put).toHaveBeenCalledWith('/knowledge-bases/1', {
        name: 'updated',
        systemPrompt: 'prompt',
      })
      expect(result).toBe(res)
    })

    it('deleteKnowledgeBase should DELETE /knowledge-bases/:id', async () => {
      const res = { code: 200, data: null }
      mockedRequest.delete.mockResolvedValue(res)
      const result = await deleteKnowledgeBase(5)
      expect(mockedRequest.delete).toHaveBeenCalledWith('/knowledge-bases/5')
      expect(result).toBe(res)
    })
  })

  describe('document management', () => {
    it('getDocuments should GET /documents with knowledgeBaseId param', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: [] })
      await getDocuments(3)
      expect(mockedRequest.get).toHaveBeenCalledWith('/documents', { params: { knowledgeBaseId: 3 } })
    })

    it('uploadDocument should POST FormData to /documents/upload', async () => {
      mockedRequest.post.mockResolvedValue({ code: 200, data: {} })
      const file = new File(['content'], 'test.pdf', { type: 'application/pdf' })
      await uploadDocument(1, file)

      expect(mockedRequest.post).toHaveBeenCalledTimes(1)
      const [url, formData, config] = mockedRequest.post.mock.calls[0]
      expect(url).toBe('/documents/upload')
      expect(formData).toBeInstanceOf(FormData)
      expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } })
    })

    it('deleteDocument should DELETE /documents/:id', async () => {
      mockedRequest.delete.mockResolvedValue({ code: 200, data: null })
      await deleteDocument(10)
      expect(mockedRequest.delete).toHaveBeenCalledWith('/documents/10')
    })

    it('getDocumentChunks should GET /documents/:id/chunks', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: [] })
      await getDocumentChunks(7)
      expect(mockedRequest.get).toHaveBeenCalledWith('/documents/7/chunks')
    })

    it('getVectorStatus should GET /documents/:id/vector-status', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: {} })
      await getVectorStatus(7)
      expect(mockedRequest.get).toHaveBeenCalledWith('/documents/7/vector-status')
    })

    it('reVectorize should POST /documents/:id/re-vectorize', async () => {
      mockedRequest.post.mockResolvedValue({ code: 200, data: null })
      await reVectorize(7)
      expect(mockedRequest.post).toHaveBeenCalledWith('/documents/7/re-vectorize')
    })

    it('batchDeleteDocuments should POST /documents/batch-delete with ids', async () => {
      mockedRequest.post.mockResolvedValue({ code: 200, data: null })
      await batchDeleteDocuments([1, 2, 3])
      expect(mockedRequest.post).toHaveBeenCalledWith('/documents/batch-delete', { ids: [1, 2, 3] })
    })

    it('getDocumentPreview should GET /documents/:id/preview', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: {} })
      await getDocumentPreview(5)
      expect(mockedRequest.get).toHaveBeenCalledWith('/documents/5/preview')
    })

    it('getDocumentDownloadUrl should GET /documents/:id/download with blob response', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: new Blob() })
      await getDocumentDownloadUrl(5)
      expect(mockedRequest.get).toHaveBeenCalledWith('/documents/5/download', { responseType: 'blob' })
    })

    it('getDocumentRaw should GET /documents/:id/raw with blob response', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: new Blob() })
      await getDocumentRaw(5)
      expect(mockedRequest.get).toHaveBeenCalledWith('/documents/5/raw', { responseType: 'blob' })
    })
  })

  describe('categories', () => {
    it('getCategories should GET /categories with knowledgeBaseId param', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: [] })
      await getCategories(2)
      expect(mockedRequest.get).toHaveBeenCalledWith('/categories', { params: { knowledgeBaseId: 2 } })
    })

    it('createCategory should POST /categories', async () => {
      mockedRequest.post.mockResolvedValue({ code: 200, data: { id: 1 } })
      await createCategory({ knowledgeBaseId: 2, name: 'cat1', parentId: null })
      expect(mockedRequest.post).toHaveBeenCalledWith('/categories', {
        knowledgeBaseId: 2,
        name: 'cat1',
        parentId: null,
      })
    })

    it('updateCategory should PUT /categories/:id', async () => {
      mockedRequest.put.mockResolvedValue({ code: 200, data: null })
      await updateCategory(1, { name: 'renamed' })
      expect(mockedRequest.put).toHaveBeenCalledWith('/categories/1', { name: 'renamed' })
    })

    it('deleteCategory should DELETE /categories/:id', async () => {
      mockedRequest.delete.mockResolvedValue({ code: 200, data: null })
      await deleteCategory(1)
      expect(mockedRequest.delete).toHaveBeenCalledWith('/categories/1')
    })
  })

  describe('tags', () => {
    it('getTags should GET /tags with knowledgeBaseId param', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: [] })
      await getTags(2)
      expect(mockedRequest.get).toHaveBeenCalledWith('/tags', { params: { knowledgeBaseId: 2 } })
    })

    it('createTag should POST /tags', async () => {
      mockedRequest.post.mockResolvedValue({ code: 200, data: { id: 1 } })
      await createTag({ knowledgeBaseId: 2, name: 'tag1', color: '#ff0000' })
      expect(mockedRequest.post).toHaveBeenCalledWith('/tags', {
        knowledgeBaseId: 2,
        name: 'tag1',
        color: '#ff0000',
      })
    })

    it('deleteTag should DELETE /tags/:id', async () => {
      mockedRequest.delete.mockResolvedValue({ code: 200, data: null })
      await deleteTag(3)
      expect(mockedRequest.delete).toHaveBeenCalledWith('/tags/3')
    })
  })

  describe('document categorization / tags / versions', () => {
    it('setDocumentCategory should PUT /documents/:id/category', async () => {
      mockedRequest.put.mockResolvedValue({ code: 200, data: null })
      await setDocumentCategory(10, 5)
      expect(mockedRequest.put).toHaveBeenCalledWith('/documents/10/category', { categoryId: 5 })
    })

    it('setDocumentCategory with null should clear category', async () => {
      mockedRequest.put.mockResolvedValue({ code: 200, data: null })
      await setDocumentCategory(10, null)
      expect(mockedRequest.put).toHaveBeenCalledWith('/documents/10/category', { categoryId: null })
    })

    it('setDocumentTags should PUT /documents/:id/tags', async () => {
      mockedRequest.put.mockResolvedValue({ code: 200, data: null })
      await setDocumentTags(10, [1, 2, 3])
      expect(mockedRequest.put).toHaveBeenCalledWith('/documents/10/tags', { tagIds: [1, 2, 3] })
    })

    it('getDocumentTags should GET /documents/:id/tags', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: [] })
      await getDocumentTags(10)
      expect(mockedRequest.get).toHaveBeenCalledWith('/documents/10/tags')
    })

    it('getDocumentVersions should GET /documents/:id/versions', async () => {
      mockedRequest.get.mockResolvedValue({ code: 200, data: [] })
      await getDocumentVersions(10)
      expect(mockedRequest.get).toHaveBeenCalledWith('/documents/10/versions')
    })

    it('rollbackDocumentVersion should POST /documents/:id/rollback/:versionNo', async () => {
      mockedRequest.post.mockResolvedValue({ code: 200, data: null })
      await rollbackDocumentVersion(10, 3)
      expect(mockedRequest.post).toHaveBeenCalledWith('/documents/10/rollback/3')
    })
  })
})
