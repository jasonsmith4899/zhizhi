<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

interface Props {
  content: string
  size?: 'sm' | 'md'
  boxed?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  boxed: false,
})
const html = computed(() => renderMarkdown(props.content))
</script>

<template>
  <div class="markdown-body" :class="[size, { boxed }]" v-html="html"></div>
</template>

<style scoped>
.boxed {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: var(--space-3);
}
</style>
