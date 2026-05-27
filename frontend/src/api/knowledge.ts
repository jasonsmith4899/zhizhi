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
