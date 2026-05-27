<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getConversations, getMessages, deleteConversation } from '../../api/chat'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const conversations = ref<any[]>([])
const selectedConv = ref<any>(null)
const messages = ref<any[]>([])
const msgLoading = ref(false)

onMounted(() => {
  loadConversations()
})

async function loadConversations() {
  loading.value = true
  try {
    const res = await getConversations()
    conversations.value = (res as any).data || []
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

async function viewMessages(conv: any) {
  selectedConv.value = conv
  msgLoading.value = true
  try {
    const res = await getMessages(conv.id)
    messages.value = (res as any).data || []
  } catch {
    // handled
  } finally {
    msgLoading.value = false
  }
}

async function handleDelete(conv: any) {
  try {
    await ElMessageBox.confirm('确定删除该对话？', '确认', { type: 'warning' })
    await deleteConversation(conv.id)
    ElMessage.success('已删除')
    if (selectedConv.value?.id === conv.id) {
      selectedConv.value = null
      messages.value = []
    }
    loadConversations()
  } catch {
    // cancelled
  }
}

function roleText(role: string) {
  return role === 'user' ? '用户' : role === 'assistant' ? 'AI助手' : '系统'
}

function roleType(role: string) {
  return role === 'user' ? 'primary' : role === 'assistant' ? 'success' : 'info'
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>对话记录</h2>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：会话列表 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span style="font-weight: 600">会话列表 ({{ conversations.length }})</span>
          </template>
          <div v-loading="loading" style="max-height: 600px; overflow-y: auto">
            <div
              v-for="conv in conversations"
              :key="conv.id"
              :style="{
                padding: '12px',
                marginBottom: '8px',
                borderRadius: '8px',
                cursor: 'pointer',
                background: selectedConv?.id === conv.id ? '#ecf5ff' : '#f5f7fa',
                border: selectedConv?.id === conv.id ? '1px solid #409eff' : '1px solid transparent',
              }"
              @click="viewMessages(conv)"
            >
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span style="font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 200px">
                  {{ conv.title || '新对话' }}
                </span>
                <el-button type="danger" link size="small" @click.stop="handleDelete(conv)">删除</el-button>
              </div>
              <div style="font-size: 12px; color: #909399; margin-top: 4px">
                {{ conv.messageCount }} 条消息 · {{ new Date(conv.updatedAt).toLocaleString('zh-CN') }}
              </div>
            </div>
            <el-empty v-if="!loading && conversations.length === 0" description="暂无对话" :image-size="80" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：消息详情 -->
      <el-col :span="16">
        <el-card v-if="selectedConv">
          <template #header>
            <span style="font-weight: 600">{{ selectedConv.title || '对话详情' }}</span>
          </template>
          <div v-loading="msgLoading" style="max-height: 600px; overflow-y: auto">
            <div v-for="msg in messages" :key="msg.id" style="margin-bottom: 16px">
              <div style="display: flex; align-items: center; margin-bottom: 4px">
                <el-tag :type="roleType(msg.role)" size="small">{{ roleText(msg.role) }}</el-tag>
                <span style="font-size: 12px; color: #909399; margin-left: 8px">
                  {{ new Date(msg.createdAt).toLocaleString('zh-CN') }}
                </span>
              </div>
              <div style="padding: 12px; border-radius: 8px; background: #f5f7fa; line-height: 1.6; white-space: pre-wrap">
                {{ msg.content }}
              </div>
            </div>
          </div>
        </el-card>
        <el-card v-else>
          <el-empty description="请从左侧选择一个对话" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
