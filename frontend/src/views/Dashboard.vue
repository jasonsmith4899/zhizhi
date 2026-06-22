<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Collection, ChatDotRound, Ticket, Plus, Setting, DataLine } from '@element-plus/icons-vue'
import { getKnowledgeBases } from '../api/knowledge'
import { getConversations } from '../api/chat'
import { useAuthStore } from '../stores/auth'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import ShineButton from '@/components/ui/ShineButton.vue'
import GlowButton from '@/components/ui/GlowButton.vue'

const router = useRouter()
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
    <PageHeader title="仪表盘" subtitle="系统概览与数据统计" decoration />

    <div class="stats-grid">
      <StatCard :icon="Collection" label="知识库数量" :value="kbCount" />
      <StatCard :icon="ChatDotRound" label="对话总数" :value="convCount" />
      <StatCard :icon="Ticket" label="当前套餐" :value="planLabel" is-text />
    </div>

    <div class="sections">
      <SectionCard title="快速操作" :icon="DataLine">
        <div class="action-grid">
          <ShineButton size="xl" :icon="Plus" @click="router.push('/knowledge')">创建知识库</ShineButton>
          <GlowButton size="xl" :icon="ChatDotRound" @click="router.push('/chat-history')">查看对话</GlowButton>
          <GlowButton size="xl" :icon="Setting" @click="router.push('/profile')">系统设置</GlowButton>
        </div>
      </SectionCard>

      <SectionCard title="快速上手" :icon="DataLine">
        <div class="steps-container">
          <el-steps :active="1" align-center>
            <el-step title="创建知识库" description="按业务场景创建知识库" />
            <el-step title="上传文档" description="上传PDF/TXT/MD文档" />
            <el-step title="配置机器人" description="设置欢迎语和Prompt" />
            <el-step title="接入小程序" description="对接微信小程序" />
          </el-steps>
        </div>
      </SectionCard>
    </div>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
  margin-bottom: var(--space-8);
}
.sections {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}
.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
}
.action-grid > * {
  width: 100%;
}
.steps-container {
  padding: var(--space-5) 0;
}

@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .action-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .action-grid { grid-template-columns: 1fr; }
}
</style>
