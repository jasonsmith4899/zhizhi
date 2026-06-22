<script setup lang="ts">
interface Props {
  type?: 'success' | 'info' | 'warning' | 'danger' | 'primary'
  label: string
  pulse?: boolean
}
withDefaults(defineProps<Props>(), {
  type: 'success',
  pulse: false,
})
</script>

<template>
  <span class="status-badge" :class="`type-${type}`">
    <span class="dot" :class="{ 'is-pulse': pulse }"></span>
    {{ label }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-pill);
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  font-size: 12px;
  border: 1px solid;
}
.dot {
  width: var(--size-dot);
  height: var(--size-dot);
  border-radius: var(--radius-round);
  background: currentColor;
  box-shadow: 0 0 8px currentColor;
}
.is-pulse { animation: blink 2s ease-in-out infinite; }
.type-success { background: var(--color-success-bg); border-color: var(--color-success-border); color: var(--color-success); }
.type-info { background: var(--color-info-bg); border-color: var(--color-info-border); color: var(--color-info); }
.type-warning { background: var(--color-warning-bg); border-color: var(--color-warning-border); color: var(--color-warning); }
.type-danger { background: var(--color-danger-bg); border-color: var(--color-danger-border); color: var(--color-danger); }
.type-primary { background: var(--overlay-primary-15); border-color: var(--overlay-primary-30); color: var(--color-neon-blue); }
</style>
