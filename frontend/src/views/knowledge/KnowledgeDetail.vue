<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getKnowledgeBase,
  getDocuments,
  uploadDocument,
  deleteDocument,

  getVectorStatus,
  reVectorize,
  batchDeleteDocuments,
  getDocumentDownloadUrl
} from '../../api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, WarningFilled, Upload, Download } from '@element-plus/icons-vue'
import DocumentPreview from '../../components/knowledge/DocumentPreview.vue'
import DocumentManageDialog from '../../components/knowledge/DocumentManageDialog.vue'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)
const kb = ref<any>(null)
const documents = ref<any[]>([])
const loading = ref(false)
const uploading = ref(false)
const activeTab = ref('documents')

// 文档预览
const previewDialogVisible = ref(false)
const previewFilename = ref('')
const previewFileType = ref('')
const previewDownloading = ref(false)
const previewDocumentId = ref<number | null>(null)

// 文档管理（分类/标签/版本）
const manageVisible = ref(false)
const manageDocId = ref<number | null>(null)
const manageCategoryId = ref<number | null>(null)

function openManage(row: any) {
  manageDocId.value = row.id
  manageCategoryId.value = row.categoryId ?? null
  manageVisible.value = true
}

// 向量化状态
const vectorStatusMap = ref<Record<number, any>>({})

// 重新向量化loading
const reVectorizeLoading = ref<Record<number, boolean>>({})

// 批量删除
const selectedDocuments = ref<any[]>([])
const batchDeleting = ref(false)

onMounted(() => {
  loadKb()
  loadDocuments()
})

async function loadKb() {
  try {
    const res = await getKnowledgeBase(kbId)
    kb.value = (res as any).data
  } catch {
    ElMessage.error('知识库不存在')
    router.push('/knowledge')
  }
}

async function loadDocuments() {
  loading.value = true
  try {
    const res = await getDocuments(kbId)
    documents.value = (res as any).data || []
    // Load vector status for each document
    for (const doc of documents.value) {
      loadVectorStatus(doc.id)
    }
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

async function loadVectorStatus(documentId: number) {
  try {
    const res = await getVectorStatus(documentId)
    vectorStatusMap.value[documentId] = (res as any).data
  } catch {
    // ignore
  }
}

async function handleUpload(options: any) {
  uploading.value = true
  try {
    await uploadDocument(kbId, options.file)
    ElMessage.success('文档上传成功，正在处理中...')
    loadDocuments()
    loadKb()
  } catch {
    // handled
  } finally {
    uploading.value = false
  }
}

async function handleDeleteDocument(id: number, filename: string) {
  try {
    await ElMessageBox.confirm(`确定删除文档「${filename}」？`, '确认删除', { type: 'warning' })
    await deleteDocument(id)
    ElMessage.success('已删除')
    loadDocuments()
    loadKb()
  } catch {
    // cancelled
  }
}

// 文档预览（原始文件渲染：PDF 原样 / Markdown / 文本）
function handlePreview(doc: any) {
  previewFilename.value = doc.filename
  previewFileType.value = doc.fileType || ''
  previewDocumentId.value = doc.id
  previewDialogVisible.value = true
}

// 文档下载
async function handleDownload() {
  if (!previewDocumentId.value) return
  previewDownloading.value = true
  try {
    const res = await getDocumentDownloadUrl(previewDocumentId.value)
    const blob = new Blob([(res as any)], { type: 'text/plain;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = previewFilename.value.replace(/\.[^/.]+$/, '') + '.txt'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch {
    ElMessage.error('下载失败')
  } finally {
    previewDownloading.value = false
  }
}

// 重新向量化
async function handleReVectorize(doc: any) {
  try {
    await ElMessageBox.confirm(`确定重新向量化文档「${doc.filename}」？`, '确认操作', { type: 'warning' })
    reVectorizeLoading.value[doc.id] = true
    await reVectorize(doc.id)
    ElMessage.success('重新向量化已启动')
    // Refresh status after a delay
    setTimeout(() => {
      loadDocuments()
      loadKb()
    }, 3000)
  } catch {
    // cancelled
  } finally {
    reVectorizeLoading.value[doc.id] = false
  }
}

// 批量删除
function handleSelectionChange(selection: any[]) {
  selectedDocuments.value = selection
}

async function handleBatchDelete() {
  if (selectedDocuments.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedDocuments.value.length} 个文档？此操作不可撤销。`,
      '批量删除',
      { type: 'warning' }
    )
    batchDeleting.value = true
    const ids = selectedDocuments.value.map((d: any) => d.id)
    await batchDeleteDocuments(ids)
    ElMessage.success('批量删除完成')
    selectedDocuments.value = []
    loadDocuments()
    loadKb()
  } catch {
    // cancelled
  } finally {
    batchDeleting.value = false
  }
}

function statusType(status: string) {
  const map: Record<string, string> = { ready: 'success', processing: 'warning', failed: 'danger' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { ready: '已就绪', processing: '处理中', failed: '失败' }
  return map[status] || status
}

function needsReVectorize(doc: any): boolean {
  const vs = vectorStatusMap.value[doc.id]
  if (!vs) return false
  return vs.needsReVectorization || (doc.status === 'ready' && doc.chunkCount === 0)
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; align-items: center">
        <el-button @click="router.push('/knowledge')" style="margin-right: 12px">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>{{ kb?.name || '加载中...' }}</h2>
      </div>
    </div>

    <!-- 知识库信息 -->
    <el-card v-if="kb" style="margin-bottom: 20px">
      <el-descriptions :column="4" border>
        <el-descriptions-item label="名称">{{ kb.name }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ kb.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文档数">{{ kb.documentCount }}</el-descriptions-item>
        <el-descriptions-item label="切片数">{{ kb.chunkCount }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" style="margin-bottom: 20px">
      <!-- 文档管理 Tab -->
      <el-tab-pane label="文档管理" name="documents">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: 600">文档列表</span>
              <div style="display: flex; gap: 8px; align-items: center">
                <el-button
                  v-if="selectedDocuments.length > 0"
                  type="danger"
                  :loading="batchDeleting"
                  @click="handleBatchDelete"
                >
                  批量删除 ({{ selectedDocuments.length }})
                </el-button>
                <el-upload
                  :http-request="handleUpload"
                  :show-file-list="false"
                  accept=".pdf,.txt,.md,.markdown"
                  :disabled="uploading"
                >
                  <el-button type="primary" :loading="uploading">
                    <el-icon style="margin-right: 4px"><Upload /></el-icon>
                    上传文档
                  </el-button>
                </el-upload>
              </div>
            </div>
          </template>

          <el-alert type="info" :closable="false" style="margin-bottom: 16px">
            支持 PDF、TXT、Markdown 格式，单文件最大 20MB。上传后系统会自动解析、切片和向量化。
          </el-alert>

          <el-table
            :data="documents"
            v-loading="loading"
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column prop="filename" label="文件名" min-width="250" />
            <el-table-column prop="fileType" label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.fileType?.toUpperCase() }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-popover
                  v-if="vectorStatusMap[row.id]"
                  placement="top"
                  :width="280"
                  trigger="hover"
                >
                  <template #reference>
                    <span style="cursor: pointer">
                      <el-tag :type="statusType(row.status)" size="small">
                        {{ statusText(row.status) }}
                      </el-tag>
                      <el-icon
                        v-if="needsReVectorize(row)"
                        style="color: var(--color-warning); margin-left: 4px; vertical-align: middle"
                      >
                        <WarningFilled />
                      </el-icon>
                    </span>
                  </template>
                  <div>
                    <p style="margin: 4px 0"><strong>文档状态：</strong>{{ statusText(vectorStatusMap[row.id]?.status) }}</p>
                    <p style="margin: 4px 0"><strong>切片数量：</strong>{{ vectorStatusMap[row.id]?.chunkCount ?? 0 }}</p>
                    <p style="margin: 4px 0"><strong>向量数量：</strong>{{ vectorStatusMap[row.id]?.vectorCount ?? 0 }}</p>
                    <p v-if="vectorStatusMap[row.id]?.errorMessage" style="margin: 4px 0; color: var(--color-danger)">
                      <strong>错误信息：</strong>{{ vectorStatusMap[row.id]?.errorMessage }}
                    </p>
                    <p v-if="needsReVectorize(row)" style="margin: 4px 0; color: var(--color-warning)">
                      <el-icon><WarningFilled /></el-icon> 建议重新向量化
                    </p>
                  </div>
                </el-popover>
                <el-tag v-else :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="chunkCount" label="切片数" width="80" align="center" />
            <el-table-column prop="createdAt" label="上传时间" width="170">
              <template #default="{ row }">
                {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="handlePreview(row)">预览</el-button>
                <el-button type="primary" link @click="openManage(row)">管理</el-button>
                <el-button
                  type="warning"
                  link
                  :loading="reVectorizeLoading[row.id]"
                  @click="handleReVectorize(row)"
                >
                  重新向量化
                </el-button>
                <el-button type="danger" link @click="handleDeleteDocument(row.id, row.filename)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && documents.length === 0" description="暂无文档，请上传" />
        </el-card>
      </el-tab-pane>

    </el-tabs>

    <!-- 文档预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="`文档预览 - ${previewFilename}`"
      width="70%"
      top="5vh"
    >
      <DocumentPreview
        :document-id="previewDocumentId"
        :filename="previewFilename"
        :file-type="previewFileType"
      />
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="previewDownloading" @click="handleDownload">
          <el-icon style="margin-right: 4px"><Download /></el-icon>
          下载文档
        </el-button>
      </template>
    </el-dialog>

    <!-- 文档管理对话框（分类/标签/版本） -->
    <DocumentManageDialog
      v-model="manageVisible"
      :document-id="manageDocId"
      :knowledge-base-id="kbId"
      :current-category-id="manageCategoryId"
      @changed="loadDocuments"
    />
  </div>
</template>

<style scoped>
.preview-info {
  margin-bottom: 12px;
}

.preview-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  padding: 16px;
  background: var(--bg-input);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

/* Element Plus 组件的深空蓝覆盖统一在 global.css，不在此处重复 */
</style>
