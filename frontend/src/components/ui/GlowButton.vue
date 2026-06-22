<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  variant?: 'secondary' | 'danger'
  size?: 'sm' | 'md' | 'lg' | 'xl'
  icon?: Component
  active?: boolean
  disabled?: boolean
}
withDefaults(defineProps<Props>(), {
  variant: 'secondary',
  size: 'md',
  active: false,
  disabled: false,
})
defineEmits<{ click: [e: MouseEvent] }>()
</script>

<template>
  <button
    class="glow-btn"
    :class="[`variant-${variant}`, `size-${size}`, { 'is-active': active }]"
    :disabled="disabled"
    @click="$emit('click', $event)"
  >
    <el-icon v-if="icon"><component :is="icon" /></el-icon>
    <slot />
  </button>
</template>

<style scoped>
.glow-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  cursor: pointer;
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  letter-spacing: 0.5px;
  border-radius: var(--radius-md);
  padding: 0 var(--space-5);
  transition: all var(--transition-normal);
}
.size-sm { height: var(--size-btn-sm); font-size: 13px; }
.size-md { height: var(--size-btn-md); font-size: 14px; }
.size-lg { height: var(--size-btn-lg); font-size: 15px; }
.size-xl { height: var(--size-btn-xl); font-size: 16px; }
.variant-secondary {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}
.variant-secondary:hover:not(:disabled),
.variant-secondary.is-active {
  background: var(--overlay-primary-10);
  border-color: var(--color-primary);
  color: var(--color-primary-light);
  box-shadow: var(--shadow-btn-secondary-hover);
}
.variant-danger {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger);
}
.variant-danger:hover:not(:disabled) {
  border-color: var(--color-danger);
  box-shadow: 0 0 20px var(--color-danger-bg);
}
.glow-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
