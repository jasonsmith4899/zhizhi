<script setup lang="ts">
interface Props {
  size?: 'sm' | 'md' | 'lg' | 'xl'
  variant?: 'gradient' | 'ghost' | 'ghost-danger'
  glow?: boolean
  radius?: 'button' | 'icon' | 'pill' | 'md'
  pulseRing?: boolean
}
withDefaults(defineProps<Props>(), {
  size: 'md',
  variant: 'gradient',
  glow: false,
  radius: 'icon',
  pulseRing: false,
})
</script>

<template>
  <div class="icon-badge" :class="[`size-${size}`, `variant-${variant}`, `radius-${radius}`]">
    <span v-if="pulseRing" class="ring"></span>
    <span v-if="glow" class="glow"></span>
    <span class="content"><slot /></span>
  </div>
</template>

<style scoped>
.icon-badge {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.size-sm { width: var(--size-icon-sm); height: var(--size-icon-sm); font-size: 16px; }
.size-md { width: var(--size-icon-md); height: var(--size-icon-md); font-size: 18px; }
.size-lg { width: var(--size-avatar-lg); height: var(--size-avatar-lg); font-size: 22px; }
.size-xl { width: var(--size-avatar-xl); height: var(--size-avatar-xl); font-size: 32px; }
.radius-md { border-radius: var(--radius-md); }
.radius-button { border-radius: var(--radius-button); }
.radius-icon { border-radius: var(--radius-icon); }
.radius-pill { border-radius: var(--radius-pill); }
.variant-gradient { background: var(--gradient-icon); color: #fff; }
.variant-ghost { background: var(--overlay-primary-10); color: var(--color-neon-blue); }
.variant-ghost-danger { background: var(--color-danger-bg); color: var(--color-danger); }
.content {
  position: relative;
  z-index: var(--z-content);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.glow {
  position: absolute;
  inset: -6px;
  border-radius: inherit;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  opacity: 0;
  transition: opacity var(--transition-normal);
}
.icon-badge:hover .glow { opacity: 0.6; }
.ring {
  position: absolute;
  inset: -4px;
  border-radius: inherit;
  border: 2px solid var(--color-neon-blue);
  animation: ring-pulse 2s ease-in-out infinite;
}
</style>
