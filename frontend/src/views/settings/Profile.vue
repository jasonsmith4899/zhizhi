<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApiKeys, createApiKey, updateApiKey, deleteApiKey } from '../../api/apiKey'
import { getKnowledgeBases } from '../../api/knowledge'
import { User, Setting, Key, Ticket } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const apiKeys = ref<any[]>([])
const knowledgeBases = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingKey = ref<any>(null)

// 表单
const form = ref({
  name: '',
  description: '',
  knowledgeBaseIds: [] as number[],
  assistantPersona: '',
  merchantBackground: '',
  answerRules: ''
})

const presetPersonas = [
  { label: '智能客服', text: '你是一个专业的智能客服助手，态度友好、耐心细致，能够准确回答用户的问题，遇到无法解答的问题时引导用户联系人工客服。' },
  { label: '产品顾问', text: '你是一位专业的产品顾问，熟悉公司所有产品线，能够根据用户需求推荐合适的产品，提供专业的购买建议。' },
  { label: '技术支持', text: '你是一名技术支持工程师，擅长排查和解决技术问题，回答时条理清晰、步骤明确，必要时提供代码示例。' },
  { label: '销售助手', text: '你是一位热情的销售助手，善于挖掘客户需求，突出产品优势和卖点，引导客户完成购买决策。' }
]

onMounted(async () => {
  authStore.fetchUser()
  await Promise.all([loadApiKeys(), loadKnowledgeBases()])
})

async function loadApiKeys() {
  try {
    const res = await listApiKeys() as any
    apiKeys.value = res.data || []
  } catch {}
}

async function loadKnowledgeBases() {
  try {
    const res = await getKnowledgeBases() as any
    knowledgeBases.value = res.data || []
  } catch {}
}

function openCreateDialog() {
  dialogMode.value = 'create'
  form.value = { name: '', description: '', knowledgeBaseIds: [], assistantPersona: '', merchantBackground: '', answerRules: '' }
  dialogVisible.value = true
}

function openEditDialog(key: any) {
  dialogMode.value = 'edit'
  editingKey.value = key
  form.value = {
    name: key.name,
    description: key.description || '',
    knowledgeBaseIds: key.knowledgeBaseIds || [],
    assistantPersona: key.assistantPersona || '',
    merchantBackground: key.merchantBackground || '',
    answerRules: key.answerRules || ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  loading.value = true
  try {
    if (dialogMode.value === 'create') {
      const res = await createApiKey(form.value) as any
      if (res.code === 200) {
        ElMessage.success('API Key 已创建，请立即复制保存，后续无法再次查看完整Key')
        // 显示完整的 key
        await ElMessageBox.alert(
          `<div style="word-break: break-all; font-family: monospace; padding: 12px; background: var(--overlay-primary-10); border-radius: 8px; border: 1px solid var(--border-color)">${res.data.keyValue}</div>`,
          '新 API Key（请复制保存）',
          { dangerouslyUseHTMLString: true, confirmButtonText: '我已保存' }
        )
      }
    } else {
      const res = await updateApiKey(editingKey.value.id, form.value) as any
      if (res.code === 200) {
        ElMessage.success('API Key 已更新')
      }
    }
    dialogVisible.value = false
    await loadApiKeys()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete(key: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除 API Key「${key.name}」？删除后将立即失效。`,
      '删除 API Key',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteApiKey(key.id)
    ElMessage.success('已删除')
    await loadApiKeys()
  } catch {}
}

function getKbName(id: number) {
  return knowledgeBases.value.find(kb => kb.id === id)?.name || `#${id}`
}

const planMap: Record<string, { name: string; color: string; docs: string; queries: string }> = {
  free: { name: '免费版', color: 'var(--color-neutral)', docs: '10篇', queries: '100次/日' },
  basic: { name: '基础版', color: 'var(--color-primary)', docs: '100篇', queries: '1000次/日' },
  pro: { name: '专业版', color: 'var(--color-neon-blue)', docs: '500篇', queries: '无限' },
  enterprise: { name: '企业版', color: 'var(--color-success)', docs: '不限', queries: '不限' },
}
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>个人设置</h2>
        <p class="header-subtitle">管理您的账户和 API 配置</p>
      </div>
    </div>

    <div class="settings-grid">
      <!-- 用户信息卡片 -->
      <div class="settings-card user-card">
        <div class="card-header">
          <div class="card-icon">
            <el-icon :size="20"><User /></el-icon>
          </div>
          <h3>基本信息</h3>
        </div>
        <div class="user-info">
          <div class="user-avatar">
            <div class="avatar-glow"></div>
            <span>{{ authStore.user?.username?.[0] || 'U' }}</span>
          </div>
          <div class="user-details">
            <div class="user-name">{{ authStore.user?.username }}</div>
            <div class="user-email">{{ authStore.user?.email }}</div>
            <div class="user-id">ID: {{ authStore.user?.id }}</div>
          </div>
        </div>
      </div>

      <!-- 套餐信息卡片 -->
      <div class="settings-card plan-card">
        <div class="card-header">
          <div class="card-icon">
            <el-icon :size="20"><Ticket /></el-icon>
          </div>
          <h3>套餐信息</h3>
        </div>
        <div v-if="authStore.user" class="plan-content">
          <div class="plan-badge" :style="{ '--plan-color': planMap[authStore.user.plan]?.color || 'var(--color-neutral)' }">
            <div class="badge-glow"></div>
            <span class="plan-name">{{ planMap[authStore.user.plan]?.name || authStore.user.plan }}</span>
          </div>
          <div class="plan-stats">
            <div class="plan-stat">
              <span class="stat-label">文档上限</span>
              <span class="stat-value">{{ planMap[authStore.user.plan]?.docs }}</span>
            </div>
            <div class="plan-divider"></div>
            <div class="plan-stat">
              <span class="stat-label">日问答量</span>
              <span class="stat-value">{{ planMap[authStore.user.plan]?.queries }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- API Key 管理 -->
    <div class="settings-card api-card">
      <div class="card-header">
        <div class="card-header-left">
          <div class="card-icon">
            <el-icon :size="20"><Key /></el-icon>
          </div>
          <h3>API Key 管理</h3>
        </div>
        <el-button type="primary" size="small" class="create-btn" @click="openCreateDialog">
          <span>+ 新建 Key</span>
        </el-button>
      </div>

      <div class="api-info">
        <div class="info-icon">ℹ️</div>
        <div class="info-content">
          API Key 用于通过 API 接口调用智能问答服务，通过 <code>X-API-Key</code> 请求头传递。
          每个 Key 可关联知识库，调用时只能访问已关联的知识库；不关联则不限定范围。
        </div>
      </div>

      <div v-if="apiKeys.length > 0" class="api-table-wrapper">
        <el-table :data="apiKeys" class="api-table">
          <el-table-column prop="name" label="名称" width="150">
            <template #default="{ row }">
              <span class="key-name">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Key" min-width="200">
            <template #default="{ row }">
              <code class="key-value">{{ row.keyValue }}</code>
            </template>
          </el-table-column>
          <el-table-column label="关联知识库" min-width="200">
            <template #default="{ row }">
              <template v-if="row.knowledgeBaseIds && row.knowledgeBaseIds.length > 0">
                <el-tag v-for="kbId in row.knowledgeBaseIds" :key="kbId" size="small" class="kb-tag">
                  {{ getKbName(kbId) }}
                </el-tag>
              </template>
              <el-tag v-else type="info" size="small">未配置</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" width="200" show-overflow-tooltip />
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              <span class="time-text">{{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" class="action-btn" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="danger" link size="small" class="action-btn action-btn-danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else class="empty-api">
        <div class="empty-icon">
          <div class="icon-glow"></div>
          <div class="icon-hex">
            <el-icon :size="32"><Key /></el-icon>
          </div>
        </div>
        <div class="empty-title">尚未创建 API Key</div>
        <el-button type="primary" @click="openCreateDialog">创建 API Key</el-button>
      </div>
    </div>

    <!-- 账户操作 -->
    <div class="settings-card danger-card">
      <div class="card-header">
        <div class="card-icon danger-icon">
          <el-icon :size="20"><Setting /></el-icon>
        </div>
        <h3>账户操作</h3>
      </div>
      <el-button type="danger" class="logout-btn" @click="authStore.logout(); $router.push('/login')">
        退出登录
      </el-button>
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建 API Key' : '编辑 API Key'"
      width="700px"
      class="api-dialog"
    >
      <el-form label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="给 Key 起个名字，如：生产环境、测试环境" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选描述" />
        </el-form-item>
        <el-form-item label="助手画像">
          <div style="width: 100%">
            <div class="preset-btns">
              <el-button
                v-for="preset in presetPersonas"
                :key="preset.label"
                size="small"
                class="preset-btn"
                @click="form.assistantPersona = preset.text"
              >{{ preset.label }}</el-button>
            </div>
            <el-input v-model="form.assistantPersona" type="textarea" :rows="3" placeholder="例：你是xx公司的智能客服助手" />
          </div>
        </el-form-item>
        <el-form-item label="商家背景">
          <el-input v-model="form.merchantBackground" type="textarea" :rows="3" placeholder="例：我是xx企业，主营xxx，成立于xxx" />
        </el-form-item>
        <el-form-item label="回答规则">
          <el-input v-model="form.answerRules" type="textarea" :rows="3" placeholder="例：不能直接回复价格，不能说可以打折" />
        </el-form-item>
        <el-form-item label="关联知识库">
          <el-select
            v-model="form.knowledgeBaseIds"
            multiple
            placeholder="请选择关联的知识库（可选）"
            style="width: 100%"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
          <div class="form-hint">可选，不选则不限定知识库范围</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 页面头部 */
.page-header {
  margin-bottom: 32px;
}

.header-content {
  position: relative;
  z-index: 1;
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

/* 设置网格 */
.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

/* 设置卡片 */
.settings-card {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  transition: all var(--transition-normal);
}

.settings-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-glow);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.card-icon {
  width: 36px;
  height: 36px;
  background: var(--overlay-primary-10);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-neon-blue);
}

.danger-icon {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.card-header h3 {
  font-family: 'Rajdhani', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-avatar {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-neon-blue));
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
}

.avatar-glow {
  position: absolute;
  inset: -10px;
  background: radial-gradient(circle, var(--color-neon-blue-glow) 0%, transparent 70%);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.user-avatar:hover .avatar-glow {
  opacity: 0.5;
}

.user-avatar span {
  font-family: 'Orbitron', monospace;
  font-size: 28px;
  font-weight: 800;
  color: white;
  text-shadow: 0 0 20px var(--overlay-white-50);
}

.user-details {
  flex: 1;
}

.user-name {
  font-family: 'Rajdhani', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-email {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.user-id {
  font-family: 'Orbitron', monospace;
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

/* 套餐信息 */
.plan-content {
  text-align: center;
}

.plan-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 32px;
  background: linear-gradient(135deg, var(--plan-color), var(--overlay-black-30));
  border-radius: var(--radius-lg);
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.badge-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, var(--plan-color) 0%, transparent 70%);
  opacity: 0.3;
}

.plan-name {
  font-family: 'Orbitron', monospace;
  font-size: 20px;
  font-weight: 700;
  color: white;
  text-shadow: 0 0 20px var(--overlay-white-50);
  position: relative;
  z-index: 1;
}

.plan-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.plan-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-family: 'Rajdhani', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-family: 'Orbitron', monospace;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.plan-divider {
  width: 1px;
  height: 40px;
  background: var(--border-color);
}

/* API 卡片 */
.api-card {
  margin-bottom: 24px;
}

.create-btn {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  border: none;
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
}

.create-btn:hover {
  background: linear-gradient(135deg, var(--color-primary-light), var(--color-primary));
  box-shadow: 0 0 20px var(--overlay-primary-30);
}

.api-info {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--overlay-primary-05);
  border: 1px solid var(--overlay-primary-10);
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}

.info-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.info-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.info-content code {
  background: var(--overlay-primary-10);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: var(--color-neon-blue);
}

/* API 表格 */
.api-table-wrapper {
  overflow-x: auto;
}

.api-table {
  width: 100%;
}

.key-name {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  color: var(--text-primary);
}

.key-value {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: var(--color-neon-blue);
  background: var(--overlay-neon-05);
  padding: 4px 8px;
  border-radius: 4px;
}

.kb-tag {
  margin: 2px;
  background: var(--overlay-primary-10);
  border-color: var(--overlay-primary-30);
  color: var(--color-primary-light);
}

.time-text {
  font-family: 'Rajdhani', sans-serif;
  font-size: 13px;
  color: var(--text-muted);
}

.action-btn {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  transition: all var(--transition-normal);
}

.action-btn:hover {
  text-shadow: 0 0 10px currentColor;
}

.action-btn-danger:hover {
  color: var(--color-danger);
}

/* 空状态 */
.empty-api {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
}

.empty-icon {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.empty-icon .icon-glow {
  position: absolute;
  inset: -15px;
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
  font-size: 16px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

/* 危险操作卡片 */
.danger-card {
  border-color: var(--overlay-danger-20);
}

.danger-card:hover {
  border-color: var(--overlay-danger-40);
  box-shadow: 0 0 20px var(--color-danger-bg);
}

.logout-btn {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger);
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  transition: all var(--transition-normal);
}

.logout-btn:hover {
  background: var(--overlay-danger-20);
  border-color: var(--overlay-danger-50);
  box-shadow: 0 0 20px var(--overlay-danger-20);
}

/* 对话框 */
.api-dialog :deep(.el-dialog) {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
}

.api-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-color);
}

.api-dialog :deep(.el-dialog__title) {
  color: var(--text-primary);
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
}

.api-dialog :deep(.el-dialog__body) {
  color: var(--text-primary);
}

.preset-btns {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.preset-btn {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-family: 'Rajdhani', sans-serif;
  font-weight: 500;
  transition: all var(--transition-normal);
}

.preset-btn:hover {
  background: var(--overlay-primary-10);
  border-color: var(--color-primary);
  color: var(--color-neon-blue);
}

.form-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

/* 动画 */
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
@media (max-width: 1024px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .user-info {
    flex-direction: column;
    text-align: center;
  }

  .plan-stats {
    flex-direction: column;
    gap: 16px;
  }

  .plan-divider {
    width: 40px;
    height: 1px;
  }
}
</style>
