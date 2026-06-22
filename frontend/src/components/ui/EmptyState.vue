<script setup lang="ts">
import type { Component } from 'vue'
import HexDecoration from '@/components/ui/HexDecoration.vue'

interface Props {
  title: string
  subtitle?: string
  icon?: Component
  text?: string
  size?: 'md' | 'lg'
  decoration?: boolean
}
withDefaults(defineProps<Props>(), {
  size: 'lg',
  decoration: false,
})
</script>

<template>
  <div class="empty-state" :class="`size-${size}`">
    <div class="empty-icon">
      <span class="icon-glow"></span>
      <HexDecoration :size="size === 'lg' ? 'lg' : 'md'" variant="icon" class="hex">
        <el-icon v-if="icon"><component :is="icon" /></el-icon>
        <span v-else-if="text" class="hex-text">{{ text }}</span>
      </HexDecoration>
    </div>
    <h3 class="empty-title gradient-text">{{ title }}</h3>
    <p v-if="subtitle" class="empty-subtitle">{{ subtitle }}</p>
    <div class="empty-actions">
      <slot name="actions" />
    </div>
    <div v-if="decoration" class="empty-decoration">
      <span class="dl"></span>
      <span class="dl"></span>
      <span class="dl"></span>
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--space-10);
}
.empty-icon {
  position: relative;
  margin-bottom: var(--space-8);
  display: inline-flex;
}
.icon-glow {
  position: absolute;
  inset: -20px;
  border-radius: var(--radius-round);
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  animation: glow-pulse-scale 3s ease-in-out infinite;
}
.hex {
  position: relative;
  z-index: var(--z-content);
  font-size: 28px;
}
.hex-text {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 700;
  font-size: 24px;
}
.empty-title {
  font-family: 'Rajdhani', sans-serif;
  font-size: 22px;
  font-weight: 600;
  margin-bottom: var(--space-2);
}
.empty-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  max-width: 400px;
}
.empty-actions {
  margin-top: var(--space-5);
}
.empty-decoration {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-6);
}
.dl {
  height: 2px;
  background: var(--gradient-decor-line);
  border-radius: 2px;
  transform-origin: center;
  animation: line-pulse 2s ease-in-out infinite;
}
.dl:nth-child(1) { width: 60px; }
.dl:nth-child(2) { width: 80px; animation-delay: 0.3s; }
.dl:nth-child(3) { width: 40px; animation-delay: 0.6s; }
.size-md {
  padding: var(--space-8);
}
.size-md .empty-title {
  font-size: 18px;
}
</style>
