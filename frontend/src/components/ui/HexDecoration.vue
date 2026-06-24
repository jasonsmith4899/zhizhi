<script setup lang="ts">
interface Props {
  size?: 'sm' | 'md' | 'lg'
  variant?: 'corner' | 'floating' | 'icon'
  float?: boolean
  opacity?: number
}
withDefaults(defineProps<Props>(), {
  size: 'md',
  variant: 'corner',
  float: true,
  opacity: 0.1,
})
</script>

<template>
  <div
    class="hex"
    :class="[`size-${size}`, { 'is-float': float }]"
    :style="{ opacity: variant === 'corner' ? opacity : 1 }"
  >
    <slot />
  </div>
</template>

<style scoped>
.hex {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  clip-path: var(--clip-hexagon);
  background: var(--gradient-icon);
  color: var(--color-text-on-accent);
}
.size-sm { width: var(--size-hex-sm); height: var(--size-hex-sm); }
.size-md { width: var(--size-hex-md); height: var(--size-hex-md); }
.size-lg { width: var(--size-hex-lg); height: var(--size-hex-lg); }
.is-float { animation: float-rotate 6s ease-in-out infinite; }
</style>
