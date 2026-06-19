<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Collection, ChatDotRound, Ticket, Plus, Setting, DataLine } from '@element-plus/icons-vue'
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
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>仪表盘</h2>
        <p class="header-subtitle">系统概览与数据统计</p>
      </div>
      <div class="header-decoration">
        <div class="decoration-dot"></div>
        <div class="decoration-line"></div>
        <div class="decoration-dot"></div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="(stat, index) in [
        { icon: Collection, label: '知识库数量', value: kbCount, color: '#0066FF' },
        { icon: ChatDotRound, label: '对话总数', value: convCount, color: '#00D4FF' },
        { icon: Ticket, label: '当前套餐', value: planLabel, color: '#7B61FF', isText: true }
      ]" :key="index" :style="{ '--card-color': stat.color }">
        <div class="stat-card-inner">
          <div class="stat-icon-wrapper">
            <el-icon :size="48" :color="stat.color">
              <component :is="stat.icon" />
            </el-icon>
            <div class="icon-glow"></div>
          </div>
          <div class="stat-info">
            <div class="stat-label">{{ stat.label }}</div>
            <div class="stat-value" :class="{ 'stat-value-text': stat.isText }">
              {{ stat.value }}
            </div>
          </div>
          <div class="stat-decoration">
            <div class="decoration-hex"></div>
          </div>
        </div>
        <div class="stat-border"></div>
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="section-card">
      <div class="section-header">
        <div class="section-icon">
          <el-icon :size="20"><DataLine /></el-icon>
        </div>
        <h3>快速操作</h3>
        <div class="section-line"></div>
      </div>
      <div class="action-grid">
        <el-button type="primary" size="large" class="action-btn" @click="$router.push('/knowledge')">
          <el-icon style="margin-right: 8px"><Plus /></el-icon>
          <span>创建知识库</span>
          <div class="btn-shine"></div>
        </el-button>
        <el-button size="large" class="action-btn action-btn-secondary" @click="$router.push('/chat-history')">
          <el-icon style="margin-right: 8px"><ChatDotRound /></el-icon>
          <span>查看对话</span>
        </el-button>
        <el-button size="large" class="action-btn action-btn-secondary" @click="$router.push('/profile')">
          <el-icon style="margin-right: 8px"><Setting /></el-icon>
          <span>系统设置</span>
        </el-button>
      </div>
    </div>

    <!-- 使用说明 -->
    <div class="section-card">
      <div class="section-header">
        <div class="section-icon">
          <el-icon :size="20"><DataLine /></el-icon>
        </div>
        <h3>快速上手</h3>
        <div class="section-line"></div>
      </div>
      <div class="steps-container">
        <el-steps :active="1" align-center class="custom-steps">
          <el-step title="创建知识库" description="按业务场景创建知识库" />
          <el-step title="上传文档" description="上传PDF/TXT/MD文档" />
          <el-step title="配置机器人" description="设置欢迎语和Prompt" />
          <el-step title="接入小程序" description="对接微信小程序" />
        </el-steps>
      </div>
    </div>

    <!-- 装饰性背景元素 -->
    <div class="dashboard-decoration">
      <div class="floating-circle circle-1"></div>
      <div class="floating-circle circle-2"></div>
      <div class="floating-hex hex-1"></div>
      <div class="floating-hex hex-2"></div>
    </div>
  </div>
</template>

<style scoped>
/* 页面头部 */
.page-header {
  margin-bottom: 32px;
  position: relative;
}

.header-content {
  position: relative;
  z-index: 1;
}

.page-header h2 {
  font-family: 'Orbitron', monospace;
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--text-primary) 0%, var(--color-neon-blue) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.header-subtitle {
  font-family: 'Rajdhani', sans-serif;
  font-size: 16px;
  color: var(--text-secondary);
  letter-spacing: 1px;
}

.header-decoration {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}

.decoration-dot {
  width: 8px;
  height: 8px;
  background: var(--color-neon-blue);
  border-radius: 50%;
  box-shadow: 0 0 10px var(--color-neon-blue-glow);
}

.decoration-line {
  width: 100px;
  height: 2px;
  background: linear-gradient(90deg, var(--color-neon-blue), transparent);
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.stat-card {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition-normal);
}

.stat-card:hover {
  transform: translateY(-8px);
}

.stat-card-inner {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  padding: 28px;
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  z-index: 1;
}

.stat-icon-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-glow {
  position: absolute;
  inset: -10px;
  background: radial-gradient(circle, var(--card-color) 0%, transparent 70%);
  opacity: 0.2;
  filter: blur(10px);
  transition: opacity var(--transition-normal);
}

.stat-card:hover .icon-glow {
  opacity: 0.4;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-family: 'Rajdhani', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.stat-value {
  font-family: 'Orbitron', monospace;
  font-size: 42px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
  text-shadow: 0 0 30px var(--card-color);
}

.stat-value-text {
  font-size: 24px;
  font-family: 'Rajdhani', sans-serif;
  font-weight: 700;
}

.stat-decoration {
  position: absolute;
  top: 20px;
  right: 20px;
  opacity: 0.1;
}

.decoration-hex {
  width: 60px;
  height: 60px;
  background: var(--card-color);
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  animation: float 6s ease-in-out infinite;
}

.stat-border {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--card-color), transparent);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.stat-card:hover .stat-border {
  opacity: 1;
}

/* 区块卡片 */
.section-card {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 28px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.section-icon {
  width: 36px;
  height: 36px;
  background: rgba(0, 102, 255, 0.1);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-neon-blue);
}

.section-header h3 {
  font-family: 'Rajdhani', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.section-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--border-color), transparent);
}

/* 操作按钮网格 */
.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.action-btn {
  height: 56px;
  font-family: 'Rajdhani', sans-serif;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  border-radius: var(--radius-md);
  position: relative;
  overflow: hidden;
  transition: all var(--transition-normal);
}

.action-btn:hover {
  transform: translateY(-2px);
}

.btn-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.action-btn:hover .btn-shine {
  left: 100%;
}

.action-btn-secondary {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.action-btn-secondary:hover {
  background: rgba(0, 102, 255, 0.1);
  border-color: var(--color-primary);
  color: var(--color-neon-blue);
  box-shadow: 0 0 20px rgba(0, 102, 255, 0.2);
}

/* 步骤条 */
.steps-container {
  padding: 20px 0;
}

.custom-steps :deep(.el-step__head) {
  border-color: var(--border-color);
}

.custom-steps :deep(.el-step__head.is-process) {
  color: var(--color-neon-blue);
  border-color: var(--color-neon-blue);
}

.custom-steps :deep(.el-step__head.is-finish) {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.custom-steps :deep(.el-step__title) {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  color: var(--text-secondary);
}

.custom-steps :deep(.el-step__title.is-process) {
  color: var(--color-neon-blue);
}

.custom-steps :deep(.el-step__title.is-finish) {
  color: var(--color-primary);
}

.custom-steps :deep(.el-step__description) {
  font-family: 'Exo 2', sans-serif;
  color: var(--text-muted);
}

/* 装饰性背景 */
.dashboard-decoration {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.floating-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(0, 102, 255, 0.1);
  animation: float 8s ease-in-out infinite;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: 10%;
  right: 5%;
  animation-delay: 0s;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: 20%;
  left: 5%;
  animation-delay: 4s;
}

.floating-hex {
  position: absolute;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.05), rgba(0, 212, 255, 0.03));
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  animation: float 10s ease-in-out infinite;
}

.hex-1 {
  width: 100px;
  height: 100px;
  top: 30%;
  left: 10%;
  animation-delay: 2s;
}

.hex-2 {
  width: 80px;
  height: 80px;
  bottom: 30%;
  right: 15%;
  animation-delay: 6s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .action-grid {
    grid-template-columns: 1fr;
  }

  .stat-value {
    font-size: 32px;
  }
}
</style>
