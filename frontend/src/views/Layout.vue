<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  DataLine,
  Collection,
  ChatDotRound,
  Document,
  Setting,
  Fold,
  Expand,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const isCollapsed = ref(false)

const iconMap: Record<string, any> = {
  DataLine,
  Collection,
  ChatDotRound,
  Document,
  Setting,
}

onMounted(() => {
  authStore.fetchUser()
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

const menuItems = [
  { path: '/dashboard', icon: 'DataLine', title: '仪表盘' },
  { path: '/knowledge', icon: 'Collection', title: '知识库' },
  { path: '/chat', icon: 'ChatDotRound', title: 'AI 对话' },
  { path: '/chat-history', icon: 'Document', title: '对话记录' },
  { path: '/profile', icon: 'Setting', title: '个人设置' },
]
</script>

<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '72px' : '240px'" class="sidebar">
      <!-- Logo区域 -->
      <div class="sidebar-header">
        <div class="logo-wrapper">
          <div class="logo-icon">
            <div class="logo-pulse"></div>
            <span class="logo-char">智</span>
          </div>
          <transition name="fade">
            <div v-if="!isCollapsed" class="logo-text">
              <div class="logo-title">智知</div>
              <div class="logo-subtitle">AI Knowledge</div>
            </div>
          </transition>
        </div>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        :default-active="route.path"
        :collapse="isCollapsed"
        class="sidebar-menu"
        router
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
          class="menu-item"
        >
          <div class="menu-icon-wrapper">
            <el-icon><component :is="iconMap[item.icon]" /></el-icon>
            <div class="menu-icon-glow"></div>
          </div>
          <template #title>
            <span class="menu-title">{{ item.title }}</span>
          </template>
        </el-menu-item>
      </el-menu>

      <!-- 侧边栏底部装饰 -->
      <div class="sidebar-footer">
        <div class="decoration-line"></div>
        <div class="version-text" v-if="!isCollapsed">v1.0.0</div>
      </div>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶部栏 -->
      <el-header class="top-header">
        <div class="header-left">
          <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
            <el-icon :size="20">
              <Fold v-if="!isCollapsed" />
              <Expand v-else />
            </el-icon>
          </div>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <div class="user-info" @click="handleLogout">
            <el-avatar :size="36" class="user-avatar">
              {{ authStore.user?.username?.[0] || 'U' }}
            </el-avatar>
            <span class="user-name">{{ authStore.user?.username || '用户' }}</span>
            <div class="user-status"></div>
          </div>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    linear-gradient(180deg, var(--overlay-primary-05) 0%, transparent 50%),
    radial-gradient(ellipse at bottom left, var(--overlay-neon-08) 0%, transparent 50%);
  pointer-events: none;
}

/* Logo区域 */
.sidebar-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-color);
  position: relative;
  z-index: 1;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-neon-blue));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.logo-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 16px;
  border: 2px solid var(--color-neon-blue);
  opacity: 0;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.1);
  }
}

.logo-char {
  font-family: 'Orbitron', monospace;
  font-size: 20px;
  font-weight: 800;
  color: white;
  text-shadow: 0 0 10px var(--overlay-white-50);
}

.logo-text {
  overflow: hidden;
}

.logo-title {
  font-family: 'Orbitron', monospace;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.logo-subtitle {
  font-family: 'Rajdhani', sans-serif;
  font-size: 11px;
  font-weight: 400;
  color: var(--color-neon-blue);
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* 导航菜单 */
.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 20px 12px;
  position: relative;
  z-index: 1;
}

.menu-item {
  margin-bottom: 8px;
  border-radius: var(--radius-md);
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.menu-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--color-primary), var(--color-neon-blue));
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.menu-item:hover {
  background: var(--overlay-primary-10);
}

.menu-item:hover::before {
  opacity: 1;
}

.menu-item.is-active {
  background: var(--overlay-primary-15);
  box-shadow: 0 0 20px var(--overlay-primary-10);
}

.menu-item.is-active::before {
  opacity: 1;
}

.menu-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
}

.menu-icon-wrapper .el-icon {
  font-size: 20px;
  color: var(--text-secondary);
  transition: color var(--transition-normal);
  position: relative;
  z-index: 1;
}

.menu-item:hover .menu-icon-wrapper .el-icon,
.menu-item.is-active .menu-icon-wrapper .el-icon {
  color: var(--color-neon-blue);
}

.menu-icon-glow {
  position: absolute;
  inset: -5px;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.menu-item:hover .menu-icon-glow,
.menu-item.is-active .menu-icon-glow {
  opacity: 0.5;
}

.menu-title {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.5px;
  color: var(--text-secondary);
  transition: color var(--transition-normal);
}

.menu-item:hover .menu-title,
.menu-item.is-active .menu-title {
  color: var(--text-primary);
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 20px;
  position: relative;
  z-index: 1;
}

.decoration-line {
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border-color), transparent);
  margin-bottom: 12px;
}

.version-text {
  font-family: 'Rajdhani', sans-serif;
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
  letter-spacing: 1px;
}

/* 主容器 */
.main-container {
  flex: 1;
  overflow: hidden;
}

/* 顶部栏 */
.top-header {
  height: var(--header-height);
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: relative;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.collapse-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal);
  color: var(--text-secondary);
}

.collapse-btn:hover {
  background: var(--overlay-primary-10);
  color: var(--color-neon-blue);
}

.breadcrumb {
  font-family: 'Rajdhani', sans-serif;
}

.breadcrumb :deep(.el-breadcrumb__item) {
  color: var(--text-secondary);
}

.breadcrumb :deep(.el-breadcrumb__inner) {
  color: var(--text-secondary);
  font-weight: 500;
}

.breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: var(--color-neon-blue);
}

.breadcrumb :deep(.el-breadcrumb__separator) {
  color: var(--text-muted);
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  position: relative;
}

.user-info:hover {
  background: var(--overlay-primary-10);
}

.user-avatar {
  background: linear-gradient(135deg, var(--color-primary), var(--color-cosmic-purple));
  font-family: 'Orbitron', monospace;
  font-weight: 700;
  font-size: 14px;
}

.user-name {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.user-status {
  width: 8px;
  height: 8px;
  background: var(--color-success);
  border-radius: 50%;
  box-shadow: var(--shadow-online);
}

/* 主内容区 */
.main-content {
  background: var(--bg-dark);
  overflow-y: auto;
  padding: 0;
  position: relative;
}

.main-content::before {
  content: '';
  position: fixed;
  top: var(--header-height);
  left: var(--sidebar-width);
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse at 80% 20%, var(--overlay-primary-05) 0%, transparent 50%),
    radial-gradient(ellipse at 20% 80%, var(--overlay-purple-03) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-normal);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    z-index: 1000;
    height: 100vh;
  }

  .user-name {
    display: none;
  }
}
</style>
