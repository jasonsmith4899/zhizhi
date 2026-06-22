<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'

interface Props {
  title: string
  subtitle?: string
  decoration?: boolean
  back?: boolean
}
withDefaults(defineProps<Props>(), {
  decoration: false,
  back: false,
})
defineEmits<{ back: [] }>()
</script>

<template>
  <div class="page-header">
    <div class="header-left">
      <div class="title-row">
        <el-button v-if="back" class="back-btn" @click="$emit('back')">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2 class="title gradient-text">{{ title }}</h2>
      </div>
      <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
      <div v-if="decoration" class="decoration">
        <span class="d-dot"></span>
        <span class="d-line"></span>
        <span class="d-dot"></span>
      </div>
    </div>
    <div class="header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-7);
}
.title-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.title {
  font-family: 'Rajdhani', sans-serif;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.subtitle {
  margin-top: var(--space-2);
  color: var(--text-secondary);
  font-size: 14px;
}
.decoration {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-3);
}
.d-dot {
  width: var(--size-dot);
  height: var(--size-dot);
  border-radius: var(--radius-round);
  background: var(--color-neon-blue);
  box-shadow: 0 0 8px var(--color-neon-blue-glow);
}
.d-line {
  width: 100px;
  height: 2px;
  background: var(--gradient-decor-line);
}
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
</style>
