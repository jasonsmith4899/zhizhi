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
  <el-container style="height: 100vh">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" style="transition: width 0.3s">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; background: #304156">
        <span v-if="!isCollapsed" style="color: #fff; font-size: 18px; font-weight: 700">智知 AI知识库</span>
        <span v-else style="color: #fff; font-size: 20px; font-weight: 700">智</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapsed"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="iconMap[item.icon]" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e6e6e6; background: #fff">
        <div style="display: flex; align-items: center">
          <el-icon style="cursor: pointer; font-size: 20px" @click="isCollapsed = !isCollapsed">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/" style="margin-left: 16px">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown @command="handleLogout">
          <span style="display: flex; align-items: center; cursor: pointer">
            <el-avatar :size="32" style="margin-right: 8px">{{ authStore.user?.username?.[0] || 'U' }}</el-avatar>
            {{ authStore.user?.username || '用户' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 主内容区 -->
      <el-main style="background: #f5f7fa; padding: 0">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.el-aside {
  background: #304156;
  overflow: hidden;
}
.el-menu {
  border-right: none;
}
</style>
