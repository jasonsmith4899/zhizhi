<script setup lang="ts">
interface Props {
  text: string
  size?: 'sm' | 'md' | 'xl'
  gradient?: 'primary-neon' | 'purple-primary'
  glow?: boolean
  online?: boolean
}
withDefaults(defineProps<Props>(), {
  size: 'md',
  gradient: 'primary-neon',
  glow: false,
  online: false,
})
</script>

<template>
  <div class="avatar-block" :class="[`size-${size}`, `grad-${gradient}`]">
    <span v-if="glow" class="glow"></span>
    <span class="text">{{ text }}</span>
    <span v-if="online" class="online-dot"></span>
  </div>
</template>

<style scoped>
.avatar-block {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-family: 'Rajdhani', sans-serif;
  font-weight: 700;
  border-radius: var(--radius-icon);
  text-shadow: 0 0 10px var(--overlay-white-50);
}
.size-sm { width: var(--size-avatar-sm); height: var(--size-avatar-sm); font-size: 14px; }
.size-md { width: var(--size-avatar-md); height: var(--size-avatar-md); font-size: 16px; }
.size-xl { width: var(--size-avatar-xl); height: var(--size-avatar-xl); font-size: 28px; border-radius: var(--radius-pill); }
.grad-primary-neon { background: var(--gradient-icon); }
.grad-purple-primary { background: var(--gradient-icon-purple); }
.glow {
  position: absolute;
  inset: -6px;
  border-radius: inherit;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  animation: glow-pulse-scale 3s ease-in-out infinite;
}
.text { position: relative; z-index: var(--z-content); }
.online-dot {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: var(--size-dot-lg);
  height: var(--size-dot-lg);
  border-radius: var(--radius-round);
  background: var(--color-success);
  box-shadow: var(--shadow-online);
  animation: blink 2s ease-in-out infinite;
}
</style>
