<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase } from '../../api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

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
    <div class="page-header">
      <h2>知识库管理</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>
        创建知识库
      </el-button>
    </div>

    <el-table :data="knowledgeBases" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="name" label="名称" min-width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="router.push(`/knowledge/${row.id}`)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column prop="documentCount" label="文档数" width="100" align="center" />
      <el-table-column prop="chunkCount" label="切片数" width="100" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'">
            {{ row.status === 'active' ? '启用' : '归档' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="router.push(`/knowledge/${row.id}`)">管理</el-button>
          <el-button type="danger" link @click="handleDelete(row.id, row.name)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && knowledgeBases.length === 0" description="暂无知识库，点击上方按钮创建" />

    <!-- 创建对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建知识库" width="480px">
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
