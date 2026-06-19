<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getConversations, getMessages, deleteConversation } from '../../api/chat'
import { ElMessage, ElMessageBox } from 'element-plus'
import { renderMarkdown } from '../../utils/markdown'

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
              :class="{ 'conv-item-selected': selectedConv?.id === conv.id }"
              class="conv-item"
              @click="viewMessages(conv)"
            >
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span style="font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 200px">
                  {{ conv.title || '新对话' }}
                </span>
                <el-button type="danger" link size="small" @click.stop="handleDelete(conv)">删除</el-button>
              </div>
              <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px">
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
                <span style="font-size: 12px; color: var(--text-muted); margin-left: 8px">
                  {{ new Date(msg.createdAt).toLocaleString('zh-CN') }}
                </span>
              </div>
              <div class="markdown-body" style="padding: 12px; border-radius: 8px; background: var(--bg-input); line-height: 1.6" v-html="renderMarkdown(msg.content)">
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

<style scoped>
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 12px 0 6px;
  font-weight: 600;
  line-height: 1.4;
}
.markdown-body :deep(h1) { font-size: 18px; }
.markdown-body :deep(h2) { font-size: 16px; }
.markdown-body :deep(h3) { font-size: 15px; }
.markdown-body :deep(p) {
  margin: 6px 0;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}
.markdown-body :deep(li) {
  margin: 3px 0;
}
.markdown-body :deep(code) {
  background: rgba(0, 212, 255, 0.1);
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 13px;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
  color: var(--color-neon-blue);
}
.markdown-body :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  padding: 10px 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}
.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  color: var(--text-primary);
}
.markdown-body :deep(strong) {
  font-weight: 600;
}
.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--border-color);
  padding: 6px 10px;
  text-align: left;
  font-size: 13px;
}
.markdown-body :deep(th) {
  background: rgba(0, 102, 255, 0.1);
  font-weight: 600;
  color: var(--text-primary);
}
.markdown-body :deep(td) {
  color: var(--text-primary);
}

/* 对话列表项 */
.conv-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  background: rgba(30, 58, 95, 0.2);
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.conv-item:hover {
  background: rgba(0, 102, 255, 0.1);
  border-color: rgba(0, 102, 255, 0.2);
}

.conv-item-selected {
  background: rgba(0, 102, 255, 0.15) !important;
  border-color: var(--color-primary) !important;
}
</style>
