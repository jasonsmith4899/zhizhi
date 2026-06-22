<script setup lang="ts">
interface Props {
  dotColor?: string
  size?: number
}
withDefaults(defineProps<Props>(), {
  dotColor: 'var(--color-neon-blue)',
  size: 10,
})
</script>

<template>
  <span class="typing" :style="{ '--dot': dotColor, '--dot-sz': size + 'px' }">
    <span></span><span></span><span></span>
  </span>
</template>

<style scoped>
.typing {
  display: inline-flex;
  gap: var(--space-2);
  padding: var(--space-2) 0;
}
.typing span {
  width: var(--dot-sz);
  height: var(--dot-sz);
  border-radius: var(--radius-round);
  background: var(--dot);
  animation: bounce 1.4s infinite ease-in-out;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
</style>
