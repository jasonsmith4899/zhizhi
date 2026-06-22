import request from './request'

export function getKnowledgeBases() {
  return request.get('/knowledge-bases')
}

export function getKnowledgeBase(id: number) {
  return request.get(`/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data: { name: string; description?: string }) {
  return request.post('/knowledge-bases', data)
}

export function updateKnowledgeBase(id: number, data: { name: string; description?: string; systemPrompt?: string }) {
  return request.put(`/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id: number) {
  return request.delete(`/knowledge-bases/${id}`)
}

// 文档管理
export function getDocuments(knowledgeBaseId: number) {
  return request.get('/documents', { params: { knowledgeBaseId } })
}

export function uploadDocument(knowledgeBaseId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('knowledgeBaseId', knowledgeBaseId.toString())
  return request.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteDocument(id: number) {
  return request.delete(`/documents/${id}`)
}

// 文档切片预览
export function getDocumentChunks(documentId: number) {
  return request.get(`/documents/${documentId}/chunks`)
}

// 向量化状态
export function getVectorStatus(documentId: number) {
  return request.get(`/documents/${documentId}/vector-status`)
}

// 重新向量化
export function reVectorize(documentId: number) {
  return request.post(`/documents/${documentId}/re-vectorize`)
}

// 批量删除
export function batchDeleteDocuments(ids: number[]) {
  return request.post('/documents/batch-delete', { ids })
}

// 文档预览（拼接的纯文本）
export function getDocumentPreview(docId: number) {
  return request.get(`/documents/${docId}/preview`)
}

// 文档下载
export function getDocumentDownloadUrl(docId: number) {
  return request.get(`/documents/${docId}/download`, { responseType: 'blob' })
}

// 原始文件（用于全格式在线预览：PDF 原样渲染等）
export function getDocumentRaw(docId: number) {
  return request.get(`/documents/${docId}/raw`, { responseType: 'blob' })
}

// ==================== 分类 ====================
export function getCategories(knowledgeBaseId: number) {
  return request.get('/categories', { params: { knowledgeBaseId } })
}
export function createCategory(data: { knowledgeBaseId: number; parentId?: number | null; name: string; sortOrder?: number }) {
  return request.post('/categories', data)
}
export function updateCategory(id: number, data: { name?: string; parentId?: number | null; sortOrder?: number }) {
  return request.put(`/categories/${id}`, data)
}
export function deleteCategory(id: number) {
  return request.delete(`/categories/${id}`)
}

// ==================== 标签 ====================
export function getTags(knowledgeBaseId: number) {
  return request.get('/tags', { params: { knowledgeBaseId } })
}
export function createTag(data: { knowledgeBaseId: number; name: string; color?: string }) {
  return request.post('/tags', data)
}
export function deleteTag(id: number) {
  return request.delete(`/tags/${id}`)
}

// ==================== 文档归类 / 标签 / 版本 ====================
export function setDocumentCategory(docId: number, categoryId: number | null) {
  return request.put(`/documents/${docId}/category`, { categoryId })
}
export function setDocumentTags(docId: number, tagIds: number[]) {
  return request.put(`/documents/${docId}/tags`, { tagIds })
}
export function getDocumentTags(docId: number) {
  return request.get(`/documents/${docId}/tags`)
}
export function getDocumentVersions(docId: number) {
  return request.get(`/documents/${docId}/versions`)
}
export function rollbackDocumentVersion(docId: number, versionNo: number) {
  return request.post(`/documents/${docId}/rollback/${versionNo}`)
}
