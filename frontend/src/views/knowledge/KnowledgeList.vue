<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase } from '../../api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Collection } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const knowledgeBases = ref<any[]>([])
const showCreateDialog = ref(false)
const newKb = ref({ name: '', description: '' })

onMounted(() => {
  loadList()
})

async function loadList() {
  loading.value = true
  try {
    const res = await getKnowledgeBases()
    knowledgeBases.value = (res as any).data || []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!newKb.value.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  try {
    await createKnowledgeBase(newKb.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    newKb.value = { name: '', description: '' }
    loadList()
  } catch {
    // handled
  }
}

async function handleDelete(id: number, name: string) {
  try {
    await ElMessageBox.confirm(`确定删除知识库「${name}」？删除后不可恢复。`, '确认删除', {
      type: 'warning',
    })
    await deleteKnowledgeBase(id)
    ElMessage.success('已删除')
    loadList()
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>知识库管理</h2>
        <p class="header-subtitle">管理您的智能知识库</p>
      </div>
      <el-button type="primary" class="create-btn" @click="showCreateDialog = true">
        <el-icon style="margin-right: 8px"><Plus /></el-icon>
        <span>创建知识库</span>
        <div class="btn-shine"></div>
      </el-button>
    </div>

    <!-- 知识库网格 -->
    <div class="kb-grid" v-loading="loading">
      <div
        v-for="kb in knowledgeBases"
        :key="kb.id"
        class="kb-card"
        @click="router.push(`/knowledge/${kb.id}`)"
      >
        <div class="kb-card-inner">
          <!-- 卡片头部 -->
          <div class="kb-header">
            <div class="kb-icon">
              <el-icon :size="24"><Collection /></el-icon>
              <div class="icon-glow"></div>
            </div>
            <div class="kb-status" :class="kb.status">
              <span class="status-dot"></span>
              {{ kb.status === 'active' ? '启用' : '归档' }}
            </div>
          </div>

          <!-- 卡片内容 -->
          <div class="kb-content">
            <h3 class="kb-name">{{ kb.name }}</h3>
            <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
          </div>

          <!-- 卡片统计 -->
          <div class="kb-stats">
            <div class="stat-item">
              <span class="stat-value">{{ kb.documentCount || 0 }}</span>
              <span class="stat-label">文档</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ kb.chunkCount || 0 }}</span>
              <span class="stat-label">切片</span>
            </div>
          </div>

          <!-- 卡片底部 -->
          <div class="kb-footer">
            <span class="kb-time">
              {{ new Date(kb.createdAt).toLocaleDateString('zh-CN') }}
            </span>
            <div class="kb-actions">
              <el-button type="primary" link class="action-btn" @click.stop="router.push(`/knowledge/${kb.id}`)">
                管理
              </el-button>
              <el-button type="danger" link class="action-btn action-btn-danger" @click.stop="handleDelete(kb.id, kb.name)">
                删除
              </el-button>
            </div>
          </div>

          <!-- 装饰元素 -->
          <div class="kb-decoration">
            <div class="decoration-hex"></div>
          </div>
        </div>
        <div class="card-border"></div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && knowledgeBases.length === 0" class="empty-state">
      <div class="empty-icon">
        <div class="icon-glow"></div>
        <div class="icon-hex">
          <el-icon :size="40"><Collection /></el-icon>
        </div>
      </div>
      <div class="empty-title">暂无知识库</div>
      <div class="empty-subtitle">点击上方按钮创建您的第一个知识库</div>
    </div>

    <!-- 创建对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建知识库" width="480px" class="create-dialog">
      <el-form :model="newKb" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="newKb.name" placeholder="例如：产品文档、常见问题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newKb.description" type="textarea" :rows="3" placeholder="简要描述知识库用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.header-content {
  flex: 1;
}

.page-header h2 {
  font-family: 'Orbitron', monospace;
  font-size: 28px;
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

.create-btn {
  height: 48px;
  padding: 0 24px;
  font-family: 'Rajdhani', sans-serif;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  border: none;
  border-radius: var(--radius-md);
  transition: all var(--transition-normal);
}

.create-btn:hover {
  background: linear-gradient(135deg, var(--color-primary-light), var(--color-primary));
  box-shadow: 0 0 30px rgba(0, 102, 255, 0.4);
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

.create-btn:hover .btn-shine {
  left: 100%;
}

/* 知识库网格 */
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.kb-card {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.kb-card:hover {
  transform: translateY(-8px);
}

.kb-card-inner {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

/* 卡片头部 */
.kb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.kb-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-neon-blue));
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  color: white;
}

.icon-glow {
  position: absolute;
  inset: -8px;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.kb-card:hover .icon-glow {
  opacity: 0.5;
}

.kb-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'Rajdhani', sans-serif;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 4px 10px;
  border-radius: 20px;
}

.kb-status.active {
  background: rgba(0, 255, 136, 0.1);
  color: #00ff88;
  border: 1px solid rgba(0, 255, 136, 0.3);
}

.kb-status.archived {
  background: rgba(123, 97, 255, 0.1);
  color: #7b61ff;
  border: 1px solid rgba(123, 97, 255, 0.3);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 10px currentColor;
}

/* 卡片内容 */
.kb-content {
  flex: 1;
  margin-bottom: 20px;
}

.kb-name {
  font-family: 'Rajdhani', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  line-height: 1.3;
}

.kb-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 卡片统计 */
.kb-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-family: 'Orbitron', monospace;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  text-shadow: 0 0 20px var(--color-neon-blue-glow);
}

.stat-label {
  font-family: 'Rajdhani', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--border-color);
}

/* 卡片底部 */
.kb-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-time {
  font-family: 'Rajdhani', sans-serif;
  font-size: 12px;
  color: var(--text-muted);
}

.kb-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  font-size: 13px;
  transition: all var(--transition-normal);
}

.action-btn:hover {
  text-shadow: 0 0 10px currentColor;
}

.action-btn-danger:hover {
  color: #ff4757;
}

/* 装饰元素 */
.kb-decoration {
  position: absolute;
  top: 20px;
  right: 20px;
  opacity: 0.05;
  pointer-events: none;
}

.decoration-hex {
  width: 80px;
  height: 80px;
  background: var(--color-neon-blue);
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  animation: float 8s ease-in-out infinite;
}

/* 卡片边框 */
.card-border {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-neon-blue));
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.kb-card:hover .card-border {
  opacity: 1;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
}

.empty-icon {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 32px;
}

.empty-icon .icon-glow {
  position: absolute;
  inset: -20px;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse-glow 3s ease-in-out infinite;
}

.empty-icon .icon-hex {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-neon-blue));
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
  color: white;
}

.empty-title {
  font-family: 'Rajdhani', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.empty-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  text-align: center;
}

/* 创建对话框 */
.create-dialog :deep(.el-dialog) {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
}

.create-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-color);
}

.create-dialog :deep(.el-dialog__title) {
  color: var(--text-primary);
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
}

.create-dialog :deep(.el-dialog__body) {
  color: var(--text-primary);
}

/* 动画 */
@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-10px) rotate(5deg);
  }
}

@keyframes pulse-glow {
  0%, 100% {
    opacity: 0.3;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.1);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }

  .create-btn {
    width: 100%;
  }

  .kb-grid {
    grid-template-columns: 1fr;
  }
}
</style>
