<script setup lang="ts">
import type { Component } from 'vue'
import { Loading } from '@element-plus/icons-vue'

interface Props {
  size?: 'md' | 'lg' | 'xl'
  loading?: boolean
  block?: boolean
  icon?: Component
  disabled?: boolean
}
withDefaults(defineProps<Props>(), {
  size: 'lg',
  loading: false,
  block: false,
  disabled: false,
})
defineEmits<{ click: [e: MouseEvent] }>()
</script>

<template>
  <button
    class="shine-btn"
    :class="[`size-${size}`, { 'is-block': block }]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span class="shine"></span>
    <span class="content">
      <el-icon v-if="loading" class="spin"><Loading /></el-icon>
      <el-icon v-else-if="icon"><component :is="icon" /></el-icon>
      <slot />
    </span>
  </button>
</template>

<style scoped>
.shine-btn {
  position: relative;
  overflow: hidden;
  border: none;
  cursor: pointer;
  background: var(--gradient-btn-primary);
  color: #fff;
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  letter-spacing: 0.5px;
  border-radius: var(--radius-md);
  padding: 0 var(--space-6);
  transition: all var(--transition-normal);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.size-md { height: var(--size-btn-md); font-size: 14px; }
.size-lg { height: var(--size-btn-lg); font-size: 16px; }
.size-xl { height: var(--size-btn-xl); font-size: 16px; }
.is-block { width: 100%; }
.shine-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-btn-primary-hover);
}
.shine-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.content {
  position: relative;
  z-index: var(--z-content);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}
.shine {
  position: absolute;
  top: 0;
  left: -120%;
  width: 80%;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--overlay-white-20), transparent);
  transform: skewX(-20deg);
  transition: left var(--transition-slow);
}
.shine-btn:hover:not(:disabled) .shine { left: 120%; }
.spin { animation: rotate 1s linear infinite; }
</style>
