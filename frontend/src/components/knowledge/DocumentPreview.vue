<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { getDocumentRaw, getDocumentPreview } from '../../api/knowledge'
import { renderMarkdown } from '../../utils/markdown'

const props = defineProps<{
  documentId: number | null
  filename: string
  fileType: string
}>()

const loading = ref(false)
const mode = ref<'pdf' | 'markdown' | 'text' | 'empty'>('empty')
const pdfUrl = ref('')
const textContent = ref('')
const errorMsg = ref('')

function revokePdf() {
  if (pdfUrl.value) {
    URL.revokeObjectURL(pdfUrl.value)
    pdfUrl.value = ''
  }
}

async function load() {
  revokePdf()
  textContent.value = ''
  errorMsg.value = ''
  mode.value = 'empty'
  if (!props.documentId) return

  loading.value = true
  const type = (props.fileType || '').toLowerCase()
  try {
    if (type === 'pdf') {
      // PDF 原样渲染：取原始字节，交给浏览器内置阅读器
      // 注意：blob 响应经拦截器后直接返回 Blob 本身
      const raw = (await getDocumentRaw(props.documentId)) as unknown as Blob
      const blob = new Blob([raw], { type: 'application/pdf' })
      pdfUrl.value = URL.createObjectURL(blob)
      mode.value = 'pdf'
    } else if (type === 'md' || type === 'markdown') {
      textContent.value = await loadRawText(props.documentId)
      mode.value = 'markdown'
    } else {
      textContent.value = await loadRawText(props.documentId)
      mode.value = 'text'
    }
  } catch (e: any) {
    errorMsg.value = e?.message || '预览加载失败'
    mode.value = 'empty'
  } finally {
    loading.value = false
  }
}

/** 优先读原始文件文本；旧文档无原始文件时回退到拼接的切片文本 */
async function loadRawText(docId: number): Promise<string> {
  try {
    const raw = (await getDocumentRaw(docId)) as unknown as Blob
    return await raw.text()
  } catch {
    const res = (await getDocumentPreview(docId)) as any
    return res?.data?.content || ''
  }
}

watch(() => props.documentId, load, { immediate: true })
onBeforeUnmount(revokePdf)
</script>

<template>
  <div v-loading="loading" class="doc-preview">
    <el-empty v-if="mode === 'empty' && !loading" :description="errorMsg || '暂无可预览内容'" />

    <iframe
      v-else-if="mode === 'pdf'"
      :src="pdfUrl"
      class="pdf-frame"
      title="PDF 预览"
    />

    <div
      v-else-if="mode === 'markdown'"
      class="preview-content"
      v-html="renderMarkdown(textContent)"
    ></div>

    <pre v-else-if="mode === 'text'" class="preview-text">{{ textContent }}</pre>
  </div>
</template>

<style scoped>
.doc-preview {
  min-height: 300px;
}
.pdf-frame {
  width: 100%;
  height: 70vh;
  border: none;
}
.preview-content {
  line-height: 1.7;
  word-break: break-word;
}
.preview-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--el-font-family, monospace);
  font-size: 14px;
  line-height: 1.7;
  margin: 0;
}
</style>
