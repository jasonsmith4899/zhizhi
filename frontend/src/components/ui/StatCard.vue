<script setup lang="ts">
import type { Component } from 'vue'
import GlassCard from '@/components/ui/GlassCard.vue'
import IconBadge from '@/components/ui/IconBadge.vue'
import HexDecoration from '@/components/ui/HexDecoration.vue'

interface Props {
  label: string
  value: string | number
  icon?: Component
  isText?: boolean
  hex?: boolean
}
withDefaults(defineProps<Props>(), {
  isText: false,
  hex: true,
})
</script>

<template>
  <GlassCard lift="lg" top-bar class="stat-card">
    <div class="stat-body">
      <IconBadge v-if="icon" size="xl" glow>
        <el-icon><component :is="icon" /></el-icon>
      </IconBadge>
      <div class="stat-info">
        <div class="stat-label">{{ label }}</div>
        <div class="stat-value" :class="{ 'is-text': isText }">{{ value }}</div>
      </div>
    </div>
    <HexDecoration v-if="hex" size="md" variant="corner" :opacity="0.08" class="stat-hex" />
  </GlassCard>
</template>

<style scoped>
.stat-card {
  position: relative;
  overflow: hidden;
}
.stat-body {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  position: relative;
  z-index: var(--z-content);
}
.stat-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.stat-label {
  font-family: 'Rajdhani', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.stat-value {
  font-family: 'Orbitron', monospace;
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
  text-shadow: 0 0 20px var(--color-neon-blue-glow);
  line-height: 1.1;
}
.stat-value.is-text {
  font-family: 'Rajdhani', sans-serif;
  font-size: 24px;
}
.stat-hex {
  position: absolute;
  top: -10px;
  right: -10px;
  z-index: var(--z-base);
}
</style>
