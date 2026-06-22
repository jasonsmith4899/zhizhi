<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCategories, createCategory, setDocumentCategory,
  getTags, createTag, getDocumentTags, setDocumentTags,
  getDocumentVersions, rollbackDocumentVersion
} from '../../api/knowledge'

const props = defineProps<{
  modelValue: boolean
  documentId: number | null
  knowledgeBaseId: number
  currentCategoryId?: number | null
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'changed'): void
}>()

const activeTab = ref('category')
const loading = ref(false)

// 分类
const categories = ref<any[]>([])
const selectedCategoryId = ref<number | null>(null)
const newCategoryName = ref('')

// 标签
const tags = ref<any[]>([])
const selectedTagIds = ref<number[]>([])
const newTagName = ref('')

// 版本
const versions = ref<any[]>([])

watch(() => props.modelValue, (visible) => {
  if (visible && props.documentId) loadAll()
})

async function loadAll() {
  loading.value = true
  try {
    const [catRes, tagRes, docTagRes, verRes] = await Promise.all([
      getCategories(props.knowledgeBaseId),
      getTags(props.knowledgeBaseId),
      getDocumentTags(props.documentId as number),
      getDocumentVersions(props.documentId as number),
    ])
    categories.value = (catRes as any).data || []
    tags.value = (tagRes as any).data || []
    selectedTagIds.value = (docTagRes as any).data || []
    versions.value = (verRes as any).data || []
    selectedCategoryId.value = props.currentCategoryId ?? null
  } catch {
    ElMessage.error('加载文档管理信息失败')
  } finally {
    loading.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}

// 分类操作
async function handleCreateCategory() {
  if (!newCategoryName.value.trim()) return
  try {
    await createCategory({ knowledgeBaseId: props.knowledgeBaseId, name: newCategoryName.value.trim() })
    newCategoryName.value = ''
    categories.value = ((await getCategories(props.knowledgeBaseId)) as any).data || []
    ElMessage.success('分类已创建')
  } catch {
    ElMessage.error('创建分类失败')
  }
}

async function handleSaveCategory() {
  try {
    await setDocumentCategory(props.documentId as number, selectedCategoryId.value)
    ElMessage.success('分类已保存')
    emit('changed')
  } catch {
    ElMessage.error('保存分类失败')
  }
}

// 标签操作
async function handleCreateTag() {
  if (!newTagName.value.trim()) return
  try {
    await createTag({ knowledgeBaseId: props.knowledgeBaseId, name: newTagName.value.trim() })
    newTagName.value = ''
    tags.value = ((await getTags(props.knowledgeBaseId)) as any).data || []
    ElMessage.success('标签已创建')
  } catch {
    ElMessage.error('创建标签失败')
  }
}

async function handleSaveTags() {
  try {
    await setDocumentTags(props.documentId as number, selectedTagIds.value)
    ElMessage.success('标签已保存')
    emit('changed')
  } catch {
    ElMessage.error('保存标签失败')
  }
}

// 版本回滚
async function handleRollback(versionNo: number) {
  try {
    await ElMessageBox.confirm(`确定回滚到版本 v${versionNo}？将用该版本内容重建切片与向量。`, '确认回滚', { type: 'warning' })
    await rollbackDocumentVersion(props.documentId as number, versionNo)
    ElMessage.success('回滚已启动，正在重新处理...')
    emit('changed')
    close()
  } catch {
    // cancelled
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="文档管理"
    width="600px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-tabs v-model="activeTab" v-loading="loading">
      <!-- 分类 -->
      <el-tab-pane label="分类" name="category">
        <el-select v-model="selectedCategoryId" placeholder="选择分类" clearable style="width: 100%">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <div style="display: flex; gap: 8px; margin-top: 12px">
          <el-input v-model="newCategoryName" placeholder="新建分类名称" />
          <el-button @click="handleCreateCategory">新建</el-button>
        </div>
        <div style="margin-top: 16px; text-align: right">
          <el-button type="primary" @click="handleSaveCategory">保存分类</el-button>
        </div>
      </el-tab-pane>

      <!-- 标签 -->
      <el-tab-pane label="标签" name="tags">
        <el-select v-model="selectedTagIds" multiple placeholder="选择标签" style="width: 100%">
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <div style="display: flex; gap: 8px; margin-top: 12px">
          <el-input v-model="newTagName" placeholder="新建标签名称" />
          <el-button @click="handleCreateTag">新建</el-button>
        </div>
        <div style="margin-top: 16px; text-align: right">
          <el-button type="primary" @click="handleSaveTags">保存标签</el-button>
        </div>
      </el-tab-pane>

      <!-- 版本 -->
      <el-tab-pane label="版本历史" name="versions">
        <el-empty v-if="versions.length === 0" description="暂无版本记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="v in versions"
            :key="v.id"
            :timestamp="new Date(v.createdAt).toLocaleString('zh-CN')"
          >
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>
                <strong>v{{ v.versionNo }}</strong>
                <span style="color: var(--el-text-color-secondary); margin-left: 8px">
                  {{ v.remark }} · 切片 {{ v.chunkCount ?? 0 }}
                </span>
              </span>
              <el-button type="primary" link @click="handleRollback(v.versionNo)">回滚到此版本</el-button>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-dialog>
</template>
