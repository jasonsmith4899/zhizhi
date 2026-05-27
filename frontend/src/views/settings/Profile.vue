<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateApiKey, revokeApiKey } from '../../api/auth'

const authStore = useAuthStore()
const apiKeyLoading = ref(false)

onMounted(() => {
  authStore.fetchUser()
})

const maskedApiKey = computed(() => {
  const key = authStore.user?.apiKey
  if (!key) return ''
  if (key.length <= 8) return key
  return key.substring(0, 4) + '****' + key.substring(key.length - 4)
})

async function handleGenerateApiKey() {
  try {
    await ElMessageBox.confirm(
      '生成新API Key后，旧Key将立即失效，确定继续？',
      '生成API Key',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    apiKeyLoading.value = true
    const res = await generateApiKey()
    if (res.code === 200) {
      ElMessage.success('API Key 已生成')
      authStore.fetchUser()
    }
  } catch (e) {
    // 用户取消
  } finally {
    apiKeyLoading.value = false
  }
}

async function handleRevokeApiKey() {
  try {
    await ElMessageBox.confirm(
      '吊销后将无法使用此API Key调用接口，确定继续？',
      '吊销API Key',
      { confirmButtonText: '确定吊销', cancelButtonText: '取消', type: 'warning' }
    )
    apiKeyLoading.value = true
    const res = await revokeApiKey()
    if (res.code === 200) {
      ElMessage.success('API Key 已吊销')
      authStore.fetchUser()
    }
  } catch (e) {
    // 用户取消
  } finally {
    apiKeyLoading.value = false
  }
}

function copyApiKey() {
  if (authStore.user?.apiKey) {
    navigator.clipboard.writeText(authStore.user.apiKey)
    ElMessage.success('API Key 已复制')
  }
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

    <!-- API Key -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span style="font-weight: 600">API Key</span>
      </template>
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        API Key 用于通过 API 接口调用智能问答服务，通过 <code>X-API-Key</code> 请求头传递。请妥善保管，不要泄露给他人。
      </el-alert>

      <div v-if="authStore.user?.apiKey">
        <el-descriptions :column="1" border style="margin-bottom: 16px">
          <el-descriptions-item label="API Key">
            <span style="font-family: monospace">{{ maskedApiKey }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="生成时间">
            {{ authStore.user?.apiKeyCreatedAt || '未知' }}
          </el-descriptions-item>
        </el-descriptions>

        <div style="display: flex; gap: 12px">
          <el-button type="primary" @click="copyApiKey">复制完整Key</el-button>
          <el-button type="warning" :loading="apiKeyLoading" @click="handleGenerateApiKey">重新生成</el-button>
          <el-button type="danger" :loading="apiKeyLoading" @click="handleRevokeApiKey">吊销</el-button>
        </div>
      </div>

      <div v-else>
        <el-empty description="尚未生成API Key">
          <el-button type="primary" :loading="apiKeyLoading" @click="handleGenerateApiKey">生成API Key</el-button>
        </el-empty>
      </div>
    </el-card>

    <!-- 操作 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span style="font-weight: 600">账户操作</span>
      </template>
      <el-button type="danger" @click="authStore.logout(); $router.push('/login')">退出登录</el-button>
    </el-card>
  </div>
</template>
