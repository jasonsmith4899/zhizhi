<script setup lang="ts">
defineProps<{
  knowledgeBases: any[]
  selectedKbId?: number
}>()

defineEmits<{
  'update:selectedKbId': [value: number | undefined]
  newChat: []
}>()
</script>

<template>
  <div class="chat-header">
    <div style="display: flex; align-items: center; gap: 16px">
      <span style="font-weight: 600; font-size: 16px">AI 对话</span>
      <el-select
        :model-value="selectedKbId"
        placeholder="通用对话"
        clearable
        size="small"
        style="width: 200px"
        @update:model-value="$emit('update:selectedKbId', $event)"
      >
        <el-option
          v-for="kb in knowledgeBases"
          :key="kb.id"
          :label="kb.name"
          :value="kb.id"
        />
      </el-select>
      <el-tag v-if="selectedKbId" type="success" size="small">RAG 模式</el-tag>
      <el-tag v-else type="info" size="small">通用对话</el-tag>
    </div>
    <el-button size="small" @click="$emit('newChat')">新对话</el-button>
  </div>
</template>

<style scoped>
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #e6e6e6;
}
</style>
