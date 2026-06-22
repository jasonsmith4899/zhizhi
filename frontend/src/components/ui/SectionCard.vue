<script setup lang="ts">
import type { Component } from 'vue'
import GlassCard from '@/components/ui/GlassCard.vue'
import IconBadge from '@/components/ui/IconBadge.vue'
import DecorLine from '@/components/ui/DecorLine.vue'

interface Props {
  title: string
  icon?: Component
  danger?: boolean
}
withDefaults(defineProps<Props>(), {
  danger: false,
})
</script>

<template>
  <GlassCard padding="lg" :glow-on-hover="danger" class="section-card">
    <div class="section-header">
      <IconBadge v-if="icon" size="sm" :variant="danger ? 'ghost-danger' : 'ghost'" radius="md">
        <el-icon><component :is="icon" /></el-icon>
      </IconBadge>
      <h3 class="section-title">{{ title }}</h3>
      <DecorLine variant="fade" class="section-line" />
      <div class="header-actions">
        <slot name="header-actions" />
      </div>
    </div>
    <slot />
  </GlassCard>
</template>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}
.section-title {
  font-family: 'Rajdhani', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}
.section-line {
  flex: 1;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}
</style>
