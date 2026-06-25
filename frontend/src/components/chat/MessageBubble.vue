<script setup lang="ts">
import { ref, computed } from 'vue'
import { renderMarkdown, extractInlineSources } from '../../utils/markdown'
import { getDocumentChunks } from '../../api/knowledge'
import { Connection } from '@element-plus/icons-vue'

interface Source {
  documentId?: number
  documentName?: string
  content?: string
  score?: number
  chunkId?: number
}

interface KagSource {
  sourceName: string
  predicate: string
  targetName: string
  confidence?: number
}

const props = defineProps<{
  message: {
    role: string
    content: string
    sources?: Source[]
    kagSources?: KagSource[]
    time: Date
  }
  isStreaming?: boolean
}>()

const emit = defineEmits<{
  openPreview: [docId: number]
}>()

// 预览相关状态
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewContent = ref('')
const previewSourceName = ref('')

// 从正文中提取内联来源，返回干净的 markdown 正文
const parsed = computed(() => extractInlineSources(props.message.content || ''))
const htmlContent = computed(() => renderMarkdown(parsed.value.cleanContent))
const inlineSources = computed(() => parsed.value.inlineSources)

// 合并来源：优先使用结构化 sources，如果没有则使用内联解析的
const allSources = computed<Source[]>(() => {
  if (props.message.sources && props.message.sources.length > 0) {
    return props.message.sources
  }
  // 将内联来源转换为统一格式
  return inlineSources.value.map(src => ({
    documentName: src.name,
    content: src.raw
  }))
})

function scoreColor(score: number): string {
  if (score > 0.8) return 'var(--color-success)'
  if (score >= 0.6) return 'var(--color-neon-blue)'
  return 'var(--color-info)'
}

function truncateContent(content: string, maxLen = 150): string {
  if (!content) return '暂无摘要'
  return content.length > maxLen ? content.slice(0, maxLen) + '...' : content
}

// 预览切片：显示目标段落的前后10%内容
async function handlePreviewSource(source: Source) {
  if (!source.documentId) {
    // 如果没有文档ID，直接显示已有的内容
    previewSourceName.value = source.documentName || '未知来源'
    previewContent.value = source.content || '暂无预览内容'
    previewVisible.value = true
    return
  }

  previewSourceName.value = source.documentName || '未知来源'
  previewLoading.value = true
  previewVisible.value = true
  previewContent.value = ''

  try {
    const res = await getDocumentChunks(source.documentId)
    const chunks = (res as any)?.data || []

    // 找到匹配的切片
    let targetChunk = null
    if (source.chunkId) {
      targetChunk = chunks.find((c: any) => c.id === source.chunkId)
    }

    // 如果没找到精确匹配，找内容最相似的
    if (!targetChunk && source.content) {
      targetChunk = chunks.find((c: any) =>
        c.content && source.content && c.content.includes(source.content.substring(0, 50))
      )
    }

    if (targetChunk && targetChunk.content) {
      // 计算前后10%的内容
      const fullContent = targetChunk.content
      const totalLength = fullContent.length
      const tenPercent = Math.ceil(totalLength * 0.1)

      // 找到目标内容在切片中的位置
      const searchStr = source.content?.substring(0, 50) || ''
      const targetIndex = fullContent.indexOf(searchStr)

      let start = 0
      let end = totalLength

      if (targetIndex >= 0) {
        // 以目标内容为中心，取前后10%
        start = Math.max(0, targetIndex - tenPercent)
        end = Math.min(totalLength, targetIndex + (source.content?.length || 0) + tenPercent)
      } else {
        // 如果找不到精确位置，取切片的前20%
        end = Math.min(totalLength, tenPercent * 2)
      }

      previewContent.value = fullContent.substring(start, end)

      // 添加省略提示
      if (start > 0) {
        previewContent.value = '...\n\n' + previewContent.value
      }
      if (end < totalLength) {
        previewContent.value = previewContent.value + '\n\n...'
      }
    } else {
      previewContent.value = source.content || '暂无预览内容'
    }
  } catch (error) {
    previewContent.value = source.content || '获取预览失败'
  } finally {
    previewLoading.value = false
  }
}
</script>

<template>
  <div class="msg-bubble" :class="{ 'msg-user': message.role === 'user', 'msg-ai': message.role === 'assistant' }">
    <!-- AI 头像 -->
    <div v-if="message.role === 'assistant'" class="ai-avatar">
      <div class="avatar-glow"></div>
      <span>AI</span>
    </div>

    <div class="msg-content-wrapper">
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

      <!-- 信息来源：放在气泡下方，有序列表，参考文献样式 -->
      <div v-if="allSources.length > 0" class="msg-references">
        <div class="references-header">
          <div class="ref-icon-wrapper">
            <span class="ref-icon">📚</span>
          </div>
          <span>信息来源</span>
        </div>
        <ol class="references-list">
          <li
            v-for="(source, index) in allSources"
            :key="index"
            class="reference-item"
            @click="handlePreviewSource(source)"
          >
            <div class="ref-content">
              <span class="ref-doc-name">{{ source.documentName || '未知文档' }}</span>
              <span v-if="source.score != null" class="ref-score" :style="{ color: scoreColor(source.score) }">
                {{ (source.score * 100).toFixed(0) }}%
              </span>
            </div>
            <div v-if="source.content" class="ref-summary">
              {{ truncateContent(source.content, 100) }}
            </div>
          </li>
        </ol>
      </div>

      <!-- 知识图谱来源 -->
      <div v-if="message.kagSources?.length" class="kag-sources">
        <div class="kag-sources-header">
          <el-icon><Connection /></el-icon>
          <span>知识图谱</span>
        </div>
        <div class="kag-triple-list">
          <div v-for="(triple, i) in message.kagSources" :key="i" class="kag-triple">
            <span class="kag-source-name">{{ triple.sourceName }}</span>
            <span class="kag-predicate">—{{ triple.predicate }}→</span>
            <span class="kag-target-name">{{ triple.targetName }}</span>
            <span v-if="triple.confidence != null" class="kag-confidence">{{ Math.round(triple.confidence * 100) }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户头像 -->
    <div v-if="message.role === 'user'" class="user-avatar">
      <span>U</span>
    </div>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      :title="`信息来源预览 - ${previewSourceName}`"
      width="600px"
      top="10vh"
    >
      <div v-loading="previewLoading" style="min-height: 150px">
        <div v-if="previewContent" class="preview-content-wrapper">
          <pre class="preview-text">{{ previewContent }}</pre>
        </div>
        <div v-else-if="!previewLoading" style="text-align: center; color: var(--text-muted); padding: 40px 0">
          暂无预览内容
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.msg-bubble {
  display: flex;
  gap: 16px;
  padding: 20px 24px;
  max-width: 85%;
  animation: fade-in-up 0.3s ease-out;
}

.msg-user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.msg-ai {
  margin-right: auto;
}

/* 头像 */
.ai-avatar,
.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
}

.ai-avatar {
  background: linear-gradient(135deg, var(--color-primary), var(--color-neon-blue));
}

.avatar-glow {
  position: absolute;
  inset: -6px;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  opacity: 0;
  animation: pulse-glow 3s ease-in-out infinite;
}

.ai-avatar:hover .avatar-glow {
  opacity: 0.6;
}

.ai-avatar span,
.user-avatar span {
  font-family: 'Orbitron', monospace;
  font-size: 14px;
  font-weight: 800;
  color: var(--color-text-on-accent);
  text-shadow: 0 0 10px var(--overlay-white-50);
}

.user-avatar {
  background: linear-gradient(135deg, var(--color-cosmic-purple), var(--color-primary));
}

/* 内容包装器 */
.msg-content-wrapper {
  flex: 1;
  min-width: 0;
}

/* 消息内容 */
.msg-content {
  padding: 16px 20px;
  border-radius: 16px;
  line-height: 1.7;
  font-size: 14px;
  position: relative;
  overflow: hidden;
}

.msg-user .msg-content {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: var(--color-text-on-accent);
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 20px var(--overlay-primary-30);
}

.msg-ai .msg-content {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-card);
}

.msg-ai .msg-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-neon-blue));
  opacity: 0.5;
}

/* ===== Markdown 正文样式 ===== */
.msg-content :deep(h1),
.msg-content :deep(h2),
.msg-content :deep(h3) {
  margin: 16px 0 8px;
  font-weight: 700;
  line-height: 1.4;
  font-family: 'Rajdhani', sans-serif;
}

.msg-content :deep(h1) { font-size: 20px; }
.msg-content :deep(h2) { font-size: 18px; }
.msg-content :deep(h3) { font-size: 16px; }

.msg-content :deep(p) {
  margin: 8px 0;
}

.msg-content :deep(ul),
.msg-content :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}

.msg-content :deep(li) {
  margin: 4px 0;
}

.msg-content :deep(code) {
  background: var(--overlay-primary-10);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  color: var(--color-neon-blue);
}

.msg-content :deep(pre) {
  background: var(--overlay-black-30);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
  border: 1px solid var(--border-color);
}

.msg-content :deep(pre code) {
  background: none;
  padding: 0;
  color: var(--text-primary);
}

.msg-content :deep(blockquote) {
  border-left: 3px solid var(--color-neon-blue);
  padding-left: 16px;
  margin: 12px 0;
  color: var(--text-secondary);
  background: var(--overlay-neon-05);
  padding: 12px 16px;
  border-radius: 0 8px 8px 0;
}

.msg-content :deep(strong) {
  font-weight: 700;
  color: var(--color-neon-blue);
}

.msg-content :deep(table) {
  border-collapse: collapse;
  margin: 12px 0;
  width: 100%;
}

.msg-content :deep(th),
.msg-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 10px 14px;
  text-align: left;
  font-size: 13px;
}

.msg-content :deep(th) {
  background: var(--overlay-primary-10);
  font-weight: 600;
  color: var(--color-neon-blue);
  font-family: 'Rajdhani', sans-serif;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* ===== 信息来源（参考文献样式） ===== */
.msg-references {
  margin-top: 16px;
  padding: 14px 18px;
  background: var(--overlay-neon-03);
  border: 1px solid var(--overlay-neon-15);
  border-radius: 10px;
  position: relative;
}

.msg-references::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--color-neon-blue), var(--color-primary));
  border-radius: 3px 0 0 3px;
}

.references-header {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-neon-blue);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Rajdhani', sans-serif;
  letter-spacing: 0.5px;
}

.ref-icon-wrapper {
  width: 26px;
  height: 26px;
  background: var(--overlay-neon-10);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ref-icon {
  font-size: 14px;
}

.references-list {
  list-style: none;
  counter-reset: ref-counter;
  padding: 0;
  margin: 0;
}

.reference-item {
  counter-increment: ref-counter;
  padding: 10px 12px 10px 40px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
  background: var(--overlay-primary-03);
  border: 1px solid transparent;
}

.reference-item:last-child {
  margin-bottom: 0;
}

.reference-item::before {
  content: counter(ref-counter);
  position: absolute;
  left: 10px;
  top: 10px;
  width: 22px;
  height: 22px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-neon-blue));
  color: var(--color-text-on-accent);
  font-size: 11px;
  font-weight: 700;
  font-family: 'Orbitron', monospace;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-shadow: 0 0 5px var(--overlay-white-30);
}

.reference-item:hover {
  background: var(--overlay-neon-08);
  border-color: var(--overlay-neon-20);
  transform: translateX(2px);
}

.ref-content {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.ref-doc-name {
  color: var(--color-neon-blue);
  font-size: 13px;
  font-weight: 600;
  font-family: 'Rajdhani', sans-serif;
  letter-spacing: 0.3px;
}

.ref-score {
  font-size: 11px;
  font-weight: 700;
  font-family: 'Orbitron', monospace;
  padding: 2px 6px;
  background: var(--overlay-black-20);
  border-radius: 4px;
  flex-shrink: 0;
}

.ref-summary {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
  padding-left: 2px;
}

/* ===== 知识图谱来源 ===== */
.kag-sources {
  margin-top: 12px;
  padding: 12px 16px;
  background: var(--overlay-neon-05);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.kag-sources-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-neon-blue);
  font-family: 'Rajdhani', sans-serif;
}

.kag-triple-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kag-triple {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: var(--overlay-primary-05);
  border-radius: 4px;
  font-size: 13px;
}

.kag-source-name,
.kag-target-name {
  color: var(--text-primary);
  font-weight: 500;
}

.kag-predicate {
  color: var(--color-neon-blue);
  font-weight: 600;
  white-space: nowrap;
}

.kag-confidence {
  margin-left: auto;
  color: var(--text-muted);
  font-size: 12px;
  font-family: 'Orbitron', monospace;
}

/* ===== 预览对话框样式 ===== */
.preview-content-wrapper {
  max-height: 60vh;
  overflow-y: auto;
}

.preview-text {
  font-family: 'Exo 2', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  padding: 16px;
  background: var(--bg-input);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

/* ===== 打字指示器 ===== */
.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-neon-blue);
  animation: bounce 1.4s infinite ease-in-out;
  box-shadow: 0 0 10px var(--color-neon-blue-glow);
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* ===== 流式光标 ===== */
.streaming-cursor {
  animation: cursor-blink 0.8s infinite;
  color: var(--color-neon-blue);
  font-weight: bold;
  text-shadow: 0 0 10px var(--color-neon-blue-glow);
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .msg-bubble {
    max-width: 95%;
    padding: 12px 16px;
  }

  .ai-avatar,
  .user-avatar {
    width: 36px;
    height: 36px;
    border-radius: 10px;
  }

  .ai-avatar span,
  .user-avatar span {
    font-size: 12px;
  }
}
</style>
