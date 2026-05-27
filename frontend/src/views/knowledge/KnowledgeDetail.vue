<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getKnowledgeBase,
  getDocuments,
  uploadDocument,
  deleteDocument,
  updateKnowledgeBase,
  getDocumentChunks,
  getVectorStatus,
  reVectorize,
  batchDeleteDocuments,
  getDocumentPreview,
  getDocumentDownloadUrl
} from '../../api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, WarningFilled, Upload, Download } from '@element-plus/icons-vue'

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
const previewContent = ref('')
const previewLoading = ref(false)
const previewFilename = ref('')
const previewChunkCount = ref(0)
const previewDownloading = ref(false)
const previewDocumentId = ref<number | null>(null)

// 向量化状态
const vectorStatusMap = ref<Record<number, any>>({})

// 重新向量化loading
const reVectorizeLoading = ref<Record<number, boolean>>({})

// 批量删除
const selectedDocuments = ref<any[]>([])
const batchDeleting = ref(false)

// 系统提示词
const systemPrompt = ref('')
const savingPrompt = ref(false)
const selectedPreset = ref('default')

const promptPresets = [
  {
    key: 'default',
    label: '默认',
    prompt: `你是「智知」AI知识库助手。请遵循以下规则：
1. 根据知识库中的参考信息回答用户问题
2. 如果参考信息中没有相关内容，请诚实说明"知识库中暂无相关信息"
3. 回答要准确、简洁、专业
4. 在回答末尾可以标注信息来源
5. 不要编造不确定的信息`,
  },
  {
    key: 'customer-service',
    label: '客服专家',
    prompt: `你是一位专业的客服专家，代表公司与客户沟通。请遵循以下原则：
1. 始终保持礼貌、耐心、友善的态度
2. 以解决客户问题为首要目标
3. 先理解客户需求，再提供解决方案
4. 如果知识库中有相关信息，优先使用；如果没有，请引导客户联系人工客服
5. 回答要有条理，必要时分步骤说明
6. 适当表达同理心，让客户感受到被重视`,
  },
  {
    key: 'product-advisor',
    label: '产品顾问',
    prompt: `你是一位专业的产品顾问，帮助用户全面了解产品。请遵循以下原则：
1. 基于知识库中的产品信息，专业、详细地介绍产品特点和优势
2. 主动引导用户了解产品的核心价值和使用场景
3. 针对用户需求，推荐最合适的产品功能或方案
4. 客观对比不同选项，帮助用户做出明智决策
5. 回答要有深度，适当引用数据和案例
6. 如果知识库中没有相关信息，诚实告知并建议用户咨询销售团队`,
  },
  {
    key: 'tutor',
    label: '教育导师',
    prompt: `你是一位循循善诱的教育导师，致力于帮助用户学习和理解知识。请遵循以下原则：
1. 用通俗易懂的语言解释复杂概念
2. 善于使用类比和举例来帮助理解
3. 循序渐进，从基础到深入
4. 鼓励用户提问，对每一个问题都给予积极回应
5. 在回答后可以提出引导性问题，促进用户深入思考
6. 基于知识库内容回答，确保信息准确可靠`,
  },
  {
    key: 'concise',
    label: '简洁回答',
    prompt: `你是一个简洁高效的AI助手。请遵循以下原则：
1. 直接回答问题，不绕弯子
2. 答案尽量简短精准，控制在3-5句话以内
3. 只提供最关键的信息，省略背景和铺垫
4. 如果知识库中没有相关信息，直接说"暂无相关信息"
5. 不使用寒暄和客套话`,
  },
]

const defaultPrompt = promptPresets[0].prompt

onMounted(() => {
  loadKb()
  loadDocuments()
})

async function loadKb() {
  try {
    const res = await getKnowledgeBase(kbId)
    kb.value = (res as any).data
    systemPrompt.value = kb.value?.systemPrompt || ''
    // 检测当前提示词是否匹配某个预设
    const matched = promptPresets.find((p) => p.prompt === systemPrompt.value)
    selectedPreset.value = matched?.key || 'default'
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

// 文档预览
async function handlePreview(doc: any) {
  previewFilename.value = doc.filename
  previewDocumentId.value = doc.id
  previewDialogVisible.value = true
  previewLoading.value = true
  previewContent.value = ''
  previewChunkCount.value = 0
  try {
    const res = await getDocumentPreview(doc.id)
    const data = (res as any).data
    previewContent.value = data?.content || ''
    previewChunkCount.value = data?.chunkCount || 0
  } catch {
    ElMessage.error('获取文档预览失败')
  } finally {
    previewLoading.value = false
  }
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

// 保存系统提示词
async function handleSavePrompt() {
  if (!kb.value) return
  savingPrompt.value = true
  try {
    await updateKnowledgeBase(kbId, {
      name: kb.value.name,
      description: kb.value.description,
      systemPrompt: systemPrompt.value
    })
    ElMessage.success('提示词保存成功')
    loadKb()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingPrompt.value = false
  }
}

function handleUseDefaultPrompt() {
  systemPrompt.value = defaultPrompt
  selectedPreset.value = 'default'
}

function handlePresetChange(key: string) {
  selectedPreset.value = key
  const preset = promptPresets.find((p) => p.key === key)
  if (preset) {
    systemPrompt.value = preset.prompt
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
                        style="color: #e6a23c; margin-left: 4px; vertical-align: middle"
                      >
                        <WarningFilled />
                      </el-icon>
                    </span>
                  </template>
                  <div>
                    <p style="margin: 4px 0"><strong>文档状态：</strong>{{ statusText(vectorStatusMap[row.id]?.status) }}</p>
                    <p style="margin: 4px 0"><strong>切片数量：</strong>{{ vectorStatusMap[row.id]?.chunkCount ?? 0 }}</p>
                    <p style="margin: 4px 0"><strong>向量数量：</strong>{{ vectorStatusMap[row.id]?.vectorCount ?? 0 }}</p>
                    <p v-if="vectorStatusMap[row.id]?.errorMessage" style="margin: 4px 0; color: #f56c6c">
                      <strong>错误信息：</strong>{{ vectorStatusMap[row.id]?.errorMessage }}
                    </p>
                    <p v-if="needsReVectorize(row)" style="margin: 4px 0; color: #e6a23c">
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
            <el-table-column label="操作" width="220" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="handlePreview(row)">预览</el-button>
                <el-button
                  v-if="row.status === 'failed' || (row.status === 'ready' && row.chunkCount === 0)"
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

      <!-- 提示词配置 Tab -->
      <el-tab-pane label="提示词配置" name="prompt">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: 600">系统提示词配置</span>
              <el-button type="primary" size="small" @click="handleUseDefaultPrompt">恢复默认</el-button>
            </div>
          </template>

          <el-alert type="info" :closable="false" style="margin-bottom: 16px">
            系统提示词用于定义AI助手的行为和回答风格。修改后，AI将按照新的提示词进行回答。
          </el-alert>

          <div style="margin-bottom: 16px">
            <div style="font-size: 14px; color: #606266; margin-bottom: 8px">预设风格：</div>
            <el-radio-group v-model="selectedPreset" @change="handlePresetChange">
              <el-radio-button
                v-for="preset in promptPresets"
                :key="preset.key"
                :value="preset.key"
              >
                {{ preset.label }}
              </el-radio-button>
            </el-radio-group>
          </div>

          <el-input
            v-model="systemPrompt"
            type="textarea"
            :rows="12"
            placeholder="请输入系统提示词..."
            maxlength="5000"
            show-word-limit
          />

          <div style="margin-top: 16px; text-align: right">
            <el-button type="primary" :loading="savingPrompt" @click="handleSavePrompt">
              保存提示词
            </el-button>
          </div>
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
      <div v-loading="previewLoading" style="min-height: 200px">
        <el-empty v-if="!previewLoading && !previewContent" description="暂无文档内容" />
        <template v-else>
          <div class="preview-info">
            <el-tag type="info" size="small">切片数: {{ previewChunkCount }}</el-tag>
          </div>
          <el-scrollbar max-height="60vh">
            <div class="preview-content">{{ previewContent }}</div>
          </el-scrollbar>
        </template>
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="previewDownloading" @click="handleDownload">
          <el-icon style="margin-right: 4px"><Download /></el-icon>
          下载文档
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.preview-info {
  margin-bottom: 12px;
}

.preview-content {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}
</style>
