<script setup lang="ts">
defineProps<{
  previewVisible: boolean
  previewLoading: boolean
  previewData: any
}>()

defineEmits<{
  'update:previewVisible': [value: boolean]
}>()
</script>

<template>
  <el-dialog
    :model-value="previewVisible"
    :title="previewData?.documentName || '文档预览'"
    width="700px"
    destroy-on-close
    @update:model-value="$emit('update:previewVisible', $event)"
  >
    <div v-if="previewLoading" v-loading="true" style="min-height: 200px"></div>
    <div v-else-if="previewData" class="preview-content">
      <div class="preview-meta">
        <span>类型：{{ previewData.contentType?.toUpperCase() || '未知' }}</span>
        <span>切片数：{{ previewData.chunkCount ?? '-' }}</span>
      </div>
      <el-scrollbar max-height="500px">
        <pre class="preview-text">{{ previewData.content }}</pre>
      </el-scrollbar>
    </div>
    <div v-else style="text-align: center; color: var(--text-muted); padding: 40px 0">
      无法加载文档内容
    </div>
  </el-dialog>
</template>

<style scoped>
.preview-content {
  font-size: 14px;
}
.preview-meta {
  display: flex;
  gap: 20px;
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 12px;
}
.preview-text {
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: var(--text-primary);
  margin: 0;
  padding: 12px;
  background: var(--bg-input);
  border-radius: 6px;
  border: 1px solid var(--border-color);
}
</style>
