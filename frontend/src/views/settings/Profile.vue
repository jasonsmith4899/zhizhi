<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApiKeys, createApiKey, updateApiKey, deleteApiKey } from '../../api/apiKey'
import { getKnowledgeBases } from '../../api/knowledge'

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
  knowledgeBaseIds: [] as number[]
})

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
  form.value = { name: '', description: '', knowledgeBaseIds: [] }
  dialogVisible.value = true
}

function openEditDialog(key: any) {
  dialogMode.value = 'edit'
  editingKey.value = key
  form.value = {
    name: key.name,
    description: key.description || '',
    knowledgeBaseIds: key.knowledgeBaseIds || []
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (form.value.knowledgeBaseIds.length === 0) {
    ElMessage.warning('请至少选择一个知识库')
    return
  }
  loading.value = true
  try {
    if (dialogMode.value === 'create') {
      const res = await createApiKey(form.value) as any
      if (res.code === 200) {
        ElMessage.success('API Key 已创建，请立即复制保存，后续无法再次查看完整Key')
        // 显示完整的 key
        await ElMessageBox.alert(
          `<div style="word-break: break-all; font-family: monospace; padding: 12px; background: #f5f7fa; border-radius: 4px">${res.data.keyValue}</div>`,
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
  free: { name: '免费版', color: '#909399', docs: '10篇', queries: '100次/日' },
  basic: { name: '基础版', color: '#409eff', docs: '100篇', queries: '1000次/日' },
  pro: { name: '专业版', color: '#e6a23c', docs: '500篇', queries: '无限' },
  enterprise: { name: '企业版', color: '#67c23a', docs: '不限', queries: '不限' },
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>个人设置</h2>
    </div>

    <el-row :gutter="20">
      <!-- 基本信息 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span style="font-weight: 600">基本信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ authStore.user?.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ authStore.user?.email }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ authStore.user?.id }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 套餐信息 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span style="font-weight: 600">套餐信息</span>
          </template>
          <div v-if="authStore.user">
            <div style="text-align: center; margin-bottom: 20px">
              <el-tag :color="planMap[authStore.user.plan]?.color" size="large" effect="dark" style="font-size: 16px; padding: 8px 24px">
                {{ planMap[authStore.user.plan]?.name || authStore.user.plan }}
              </el-tag>
            </div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="文档上限">{{ planMap[authStore.user.plan]?.docs }}</el-descriptions-item>
              <el-descriptions-item label="日问答量">{{ planMap[authStore.user.plan]?.queries }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- API Key 管理 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">API Key 管理</span>
          <el-button type="primary" size="small" @click="openCreateDialog">+ 新建 Key</el-button>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        API Key 用于通过 API 接口调用智能问答服务，通过 <code>X-API-Key</code> 请求头传递。
        每个 Key 必须关联至少一个知识库，调用时只能访问已关联的知识库。
      </el-alert>

      <el-table v-if="apiKeys.length > 0" :data="apiKeys" stripe>
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column label="Key" min-width="200">
          <template #default="{ row }">
            <code style="font-size: 13px">{{ row.keyValue }}</code>
          </template>
        </el-table-column>
        <el-table-column label="关联知识库" min-width="200">
          <template #default="{ row }">
            <template v-if="row.knowledgeBaseIds && row.knowledgeBaseIds.length > 0">
              <el-tag v-for="kbId in row.knowledgeBaseIds" :key="kbId" size="small" style="margin: 2px">
                {{ getKbName(kbId) }}
              </el-tag>
            </template>
            <el-tag v-else type="danger" size="small">未配置</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="尚未创建 API Key">
        <el-button type="primary" @click="openCreateDialog">创建 API Key</el-button>
      </el-empty>
    </el-card>

    <!-- 操作 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span style="font-weight: 600">账户操作</span>
      </template>
      <el-button type="danger" @click="authStore.logout(); $router.push('/login')">退出登录</el-button>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建 API Key' : '编辑 API Key'"
      width="500px"
    >
      <el-form label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="给 Key 起个名字，如：生产环境、测试环境" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选描述" />
        </el-form-item>
        <el-form-item label="关联知识库">
          <el-select
            v-model="form.knowledgeBaseIds"
            multiple
            placeholder="请选择关联的知识库（至少选一个）"
            style="width: 100%"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
          <div style="font-size: 12px; color: #999; margin-top: 4px">
            必须至少关联一个知识库
          </div>
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
