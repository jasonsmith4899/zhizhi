<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Collection, ChatDotRound, Ticket, Plus, Setting } from '@element-plus/icons-vue'
import { getKnowledgeBases } from '../api/knowledge'
import { getConversations } from '../api/chat'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const kbCount = ref(0)
const convCount = ref(0)

const planLabel = computed(() => {
  const map: Record<string, string> = { free: '免费版', basic: '基础版', pro: '专业版', enterprise: '企业版' }
  return map[authStore.user?.plan || 'free'] || '免费版'
})

onMounted(async () => {
  try {
    await authStore.fetchUser()
    const [kbRes, convRes] = await Promise.all([
      getKnowledgeBases(),
      getConversations(),
    ])
    kbCount.value = (kbRes as any).data?.length || 0
    convCount.value = (convRes as any).data?.length || 0
  } catch (e) {
    console.error('Dashboard load failed:', e)
    ElMessage.warning('数据加载失败')
  }
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>仪表盘</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 24px">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div style="display: flex; align-items: center">
            <el-icon :size="48" color="#409eff" style="margin-right: 16px"><Collection /></el-icon>
            <div>
              <div class="label">知识库数量</div>
              <div class="value">{{ kbCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div style="display: flex; align-items: center">
            <el-icon :size="48" color="#67c23a" style="margin-right: 16px"><ChatDotRound /></el-icon>
            <div>
              <div class="label">对话总数</div>
              <div class="value">{{ convCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div style="display: flex; align-items: center">
            <el-icon :size="48" color="#e6a23c" style="margin-right: 16px"><Ticket /></el-icon>
            <div>
              <div class="label">当前套餐</div>
              <div class="value" style="font-size: 22px">{{ planLabel }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 -->
    <el-card style="margin-bottom: 24px">
      <template #header>
        <span style="font-weight: 600">快速操作</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-button type="primary" size="large" style="width: 100%" @click="$router.push('/knowledge')">
            <el-icon style="margin-right: 8px"><Plus /></el-icon>
            创建知识库
          </el-button>
        </el-col>
        <el-col :span="8">
          <el-button size="large" style="width: 100%" @click="$router.push('/chat-history')">
            <el-icon style="margin-right: 8px"><ChatDotRound /></el-icon>
            查看对话
          </el-button>
        </el-col>
        <el-col :span="8">
          <el-button size="large" style="width: 100%" @click="$router.push('/profile')">
            <el-icon style="margin-right: 8px"><Setting /></el-icon>
            系统设置
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 使用说明 -->
    <el-card>
      <template #header>
        <span style="font-weight: 600">快速上手</span>
      </template>
      <el-steps :active="1" align-center>
        <el-step title="创建知识库" description="按业务场景创建知识库" />
        <el-step title="上传文档" description="上传PDF/TXT/MD文档" />
        <el-step title="配置机器人" description="设置欢迎语和Prompt" />
        <el-step title="接入小程序" description="对接微信小程序" />
      </el-steps>
    </el-card>
  </div>
</template>
