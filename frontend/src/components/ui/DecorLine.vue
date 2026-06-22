<script setup lang="ts">
interface Props {
  variant?: 'gradient' | 'fade' | 'sweep'
  height?: number
}
withDefaults(defineProps<Props>(), {
  variant: 'gradient',
  height: 2,
})
</script>

<template>
  <span class="decor-line" :class="`variant-${variant}`" :style="{ height: height + 'px' }"></span>
</template>

<style scoped>
.decor-line {
  display: block;
  width: 100%;
  border-radius: 2px;
}
.variant-gradient { background: var(--gradient-decor-line); }
.variant-fade { background: linear-gradient(90deg, var(--color-primary), transparent); }
.variant-sweep { background: var(--gradient-decor-line); animation: line-move 3s ease-in-out infinite; }
</style>
