<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  score: number
  showPercent?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  showPercent: true,
})

const tone = computed(() => (props.score > 0.8 ? 'high' : props.score >= 0.6 ? 'mid' : 'low'))
const text = computed(() =>
  props.showPercent ? `${(props.score * 100).toFixed(0)}%` : props.score.toFixed(2)
)
</script>

<template>
  <span class="score-tag" :class="`tone-${tone}`">{{ text }}</span>
</template>

<style scoped>
.score-tag {
  font-family: 'Orbitron', monospace;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  background: var(--overlay-black-20);
}
.tone-high { color: var(--color-success); }
.tone-mid { color: var(--color-neon-blue); }
.tone-low { color: var(--color-info); }
</style>
