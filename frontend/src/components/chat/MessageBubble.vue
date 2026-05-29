<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown, extractInlineSources } from '../../utils/markdown'

interface Source {
  documentId?: number
  documentName?: string
  content?: string
  score?: number
}

const props = defineProps<{
  message: {
    role: string
    content: string
    sources?: Source[]
    time: Date
  }
  isStreaming?: boolean
}>()

const emit = defineEmits<{
  openPreview: [docId: number]
}>()

// 从正文中提取内联来源，返回干净的 markdown 正文
const parsed = computed(() => extractInlineSources(props.message.content || ''))
const htmlContent = computed(() => renderMarkdown(parsed.value.cleanContent))
const inlineSources = computed(() => parsed.value.inlineSources)

// 匹配内联来源到 sources 数组（按文档名模糊匹配）
function matchSource(name: string): Source | undefined {
  if (!props.message.sources) return undefined
  const normalized = name.toLowerCase().replace(/[\s\-_]/g, '')
  return props.message.sources.find(s => {
    const sName = (s.documentName || '').toLowerCase().replace(/[\s\-_]/g, '')
    return sName.includes(normalized) || normalized.includes(sName)
  })
}

function scoreColor(score: number): string {
  if (score > 0.8) return '#67c23a'
  if (score >= 0.6) return '#e6a23c'
  return '#909399'
}

function truncateContent(content: string, maxLen = 150): string {
  if (!content) return '暂无摘要'
  return content.length > maxLen ? content.slice(0, maxLen) + '...' : content
}
</script>

<template>
  <div class="msg-bubble">
    <!-- 打字指示器：内容为空且正在加载时显示 -->
    <div v-if="isStreaming && !message.content" class="typing-indicator">
      <span></span><span></span><span></span>
    </div>

    <!-- 消息正文：HTML 渲染 -->
    <div
      v-else
      class="msg-content markdown-body"
      v-html="htmlContent"
    />
    <span v-if="isStreaming && message.content" class="streaming-cursor">|</span>

    <!-- 内联信息来源（从正文中解析出来的） -->
    <div v-if="inlineSources.length > 0" class="msg-inline-sources">
      <div class="inline-sources-header">
        <span class="source-icon">📎</span> 信息来源
      </div>
      <div v-for="(src, si) in inlineSources" :key="si" class="inline-source-item">
        <el-popover
          placement="top"
          :width="280"
          trigger="click"
          :show-after="0"
          :disabled="!matchSource(src.name)"
        >
          <template #reference>
            <span
              class="inline-source-link"
              :class="{ 'has-link': matchSource(src.name) }"
            >
              {{ src.name }}
            </span>
          </template>
          <div class="source-summary">
            <div class="summary-label">摘要</div>
            <div class="summary-text">{{ truncateContent(matchSource(src.name)?.content || '') }}</div>
          </div>
        </el-popover>
      </div>
    </div>

    <!-- 推理链路（结构化 sources 数据） -->
    <div v-if="message.sources && message.sources.length > 0" class="msg-sources">
      <div class="sources-header">
        <span class="source-icon">🔗</span> 推理链路
      </div>
      <div v-for="(s, si) in message.sources" :key="si" class="source-item">
        <el-popover
          placement="top"
          :width="280"
          trigger="click"
          :show-after="0"
        >
          <template #reference>
            <span class="source-doc-link">
              {{ s.documentName || '未知文档' }}
            </span>
          </template>
          <div class="source-summary">
            <div class="summary-label">匹配摘要</div>
            <div class="summary-text">{{ truncateContent(s.content || '') }}</div>
          </div>
        </el-popover>
        <span
          v-if="s.score != null"
          class="source-score"
          :style="{ color: scoreColor(s.score) }"
        >
          {{ (s.score * 100).toFixed(0) }}%
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.msg-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
}

/* ===== Markdown 正文样式 ===== */
.msg-content :deep(h1),
.msg-content :deep(h2),
.msg-content :deep(h3) {
  margin: 12px 0 6px;
  font-weight: 600;
  line-height: 1.4;
}
.msg-content :deep(h1) { font-size: 18px; }
.msg-content :deep(h2) { font-size: 16px; }
.msg-content :deep(h3) { font-size: 15px; }
.msg-content :deep(p) {
  margin: 6px 0;
}
.msg-content :deep(ul),
.msg-content :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}
.msg-content :deep(li) {
  margin: 3px 0;
}
.msg-content :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 13px;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
}
.msg-content :deep(pre) {
  background: rgba(0, 0, 0, 0.06);
  padding: 10px 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}
.msg-content :deep(pre code) {
  background: none;
  padding: 0;
}
.msg-content :deep(blockquote) {
  border-left: 3px solid #dcdfe6;
  padding-left: 12px;
  margin: 8px 0;
  color: #909399;
}
.msg-content :deep(strong) {
  font-weight: 600;
}
.msg-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}
.msg-content :deep(th),
.msg-content :deep(td) {
  border: 1px solid #ebeef5;
  padding: 6px 10px;
  text-align: left;
  font-size: 13px;
}
.msg-content :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

/* ===== 信息来源（内联解析） ===== */
.msg-inline-sources {
  margin-top: 10px;
  padding: 8px 12px;
  background: #fdf6ec;
  border-left: 3px solid #e6a23c;
  border-radius: 4px;
}
.inline-sources-header {
  font-size: 12px;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.source-icon {
  font-size: 13px;
}
.inline-source-item {
  display: inline-block;
  margin: 2px 6px 2px 0;
}
.inline-source-link {
  color: #e6a23c;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 1px 6px;
  border-radius: 3px;
  transition: background 0.2s;
}
.inline-source-link.has-link {
  text-decoration: underline;
}
.inline-source-link:hover {
  background: rgba(230, 162, 60, 0.1);
}

/* ===== 推理链路（结构化数据） ===== */
.msg-sources {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f0f7ff;
  border-left: 3px solid #409eff;
  border-radius: 4px;
}
.sources-header {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.source-item {
  font-size: 13px;
  color: #606266;
  padding: 3px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.source-doc-link {
  color: #409eff;
  text-decoration: underline;
  cursor: pointer;
  font-weight: 500;
}
.source-doc-link:hover {
  color: #337ecc;
}
.source-score {
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

/* ===== 摘要弹窗 ===== */
.source-summary {
  max-height: 200px;
}
.summary-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  font-weight: 600;
}
.summary-text {
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===== 打字指示器 ===== */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
  animation: bounce 1.4s infinite ease-in-out;
}
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* ===== 流式光标 ===== */
.streaming-cursor {
  animation: blink 0.8s infinite;
  color: #409eff;
  font-weight: bold;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
