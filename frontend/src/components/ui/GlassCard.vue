<script setup lang="ts">
interface Props {
  lift?: 'none' | 'sm' | 'lg'
  padding?: 'none' | 'sm' | 'md' | 'lg'
  radius?: 'lg' | 'xl'
  topBar?: boolean
  clickable?: boolean
  glowOnHover?: boolean
}
withDefaults(defineProps<Props>(), {
  lift: 'none',
  padding: 'md',
  radius: 'lg',
  topBar: false,
  clickable: false,
  glowOnHover: false,
})
</script>

<template>
  <div
    class="glass-card"
    :class="[
      `lift-${lift}`,
      `pad-${padding}`,
      `radius-${radius}`,
      { 'has-topbar': topBar, 'is-clickable': clickable, 'glow-hover': glowOnHover },
    ]"
  >
    <span v-if="topBar" class="top-bar"></span>
    <slot />
  </div>
</template>

<style scoped>
.glass-card {
  position: relative;
  background: var(--bg-card);
  backdrop-filter: blur(var(--blur-glass));
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-card);
  transition: all var(--transition-normal);
}
.radius-lg { border-radius: var(--radius-lg); }
.radius-xl { border-radius: var(--radius-xl); }
.pad-none { padding: 0; }
.pad-sm { padding: var(--space-4); }
.pad-md { padding: var(--space-6); }
.pad-lg { padding: var(--space-7); }
.is-clickable { cursor: pointer; }
.top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gradient-decor-line);
  opacity: 0;
  transition: opacity var(--transition-normal);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
}
.lift-sm:hover { transform: translateY(-4px); box-shadow: var(--shadow-neon); border-color: var(--border-glow); }
.lift-lg:hover { transform: translateY(-8px); box-shadow: var(--shadow-glow); border-color: var(--border-glow); }
.has-topbar:hover .top-bar { opacity: 1; }
.glow-hover:hover { box-shadow: var(--shadow-glow); border-color: var(--border-glow); }
</style>
