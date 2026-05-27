<script setup lang="ts">
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

function scoreColor(score: number): string {
  if (score > 0.8) return '#67c23a'
  if (score >= 0.6) return '#e6a23c'
  return '#909399'
}

function truncateContent(content: string, maxLen = 200): string {
  if (!content) return ''
  return content.length > maxLen ? content.slice(0, maxLen) + '...' : content
}
</script>

<template>
  <div class="msg-bubble">
    <div class="msg-content" style="white-space: pre-wrap"
      >{{ message.content
      }}<span v-if="isStreaming" class="streaming-cursor">|</span></div
    >
    <div v-if="message.sources && message.sources.length > 0" class="msg-sources">
      <div class="sources-header">推理链路</div>
      <div v-for="(s, si) in message.sources" :key="si" class="source-item">
        <el-tooltip
          :content="truncateContent(s.content || '')"
          placement="top"
          :show-after="300"
          :disabled="!s.content"
          popper-class="source-tooltip"
        >
          <span
            class="source-doc-link"
            @click="s.documentId && emit('openPreview', s.documentId)"
          >
            {{ s.documentName || '未知文档' }}
          </span>
        </el-tooltip>
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
.msg-sources {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f0f7ff;
  border-left: 3px solid #409eff;
  border-radius: 4px;
}
.sources-header {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 6px;
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
