<script setup lang="ts">
/**
 * KgRelationTable — 关系管理表格
 * 支持分页、搜索、置信度筛选
 */
import { ref, watch } from 'vue'
import { Search, Refresh, ArrowRight } from '@element-plus/icons-vue'
import type { KgRelation } from '@/api/kg'
import { getKgRelations } from '@/api/kg'
import ScoreTag from '@/components/ui/ScoreTag.vue'

const props = defineProps<{ kbId: number }>()

const emit = defineEmits<{
  viewEntity: [entityId: number]
}>()

// ---------- 状态 ----------
const loading = ref(false)
const relations = ref<KgRelation[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = ref('')
const minConfidence = ref<number | undefined>(undefined)

// 置信度筛选选项
const confidenceOptions = [
  { label: '全部置信度', value: undefined },
  { label: '>= 90%', value: 0.9 },
  { label: '>= 70%', value: 0.7 },
  { label: '>= 50%', value: 0.5 },
]

// ---------- 数据加载 ----------
async function loadRelations() {
  loading.value = true
  try {
    const res = await getKgRelations(props.kbId, {
      page: page.value - 1, // 后端分页从 0 开始
      size: size.value,
      search: search.value || undefined,
      minConfidence: minConfidence.value,
    })
    const data = (res as any).data
    relations.value = data.content
    total.value = data.totalElements
  } catch {
    // 已由拦截器处理
  } finally {
    loading.value = false
  }
}

// ---------- 事件处理 ----------
function handleSearch() {
  page.value = 1
  loadRelations()
}

function handleRefresh() {
  search.value = ''
  minConfidence.value = undefined
  page.value = 1
  loadRelations()
}

function handlePageChange(newPage: number) {
  page.value = newPage
  loadRelations()
}

function handleSizeChange(newSize: number) {
  size.value = newSize
  page.value = 1
  loadRelations()
}

// 格式化时间
function formatTime(val: string): string {
  if (!val) return '-'
  return new Date(val).toLocaleString('zh-CN')
}

// 监听 kbId 变化
watch(() => props.kbId, () => {
  page.value = 1
  loadRelations()
}, { immediate: true })
</script>

<template>
  <div class="relation-table-wrap">
    <!-- 搜索工具栏 -->
    <div class="table-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="search"
          placeholder="搜索实体或谓词..."
          clearable
          :prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="minConfidence"
          placeholder="置信度筛选"
          clearable
          style="width: 160px"
          @change="handleSearch"
        >
          <el-option
            v-for="opt in confidenceOptions"
            :key="opt.value ?? 'all'"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>
      <el-button :icon="Refresh" circle @click="handleRefresh" />
    </div>

    <!-- 关系表格 -->
    <el-table :data="relations" v-loading="loading" stripe>
      <el-table-column prop="sourceName" label="源实体" min-width="140">
        <template #default="{ row }">
          <span class="entity-link">
            {{ row.sourceName }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="谓词" width="160" align="center">
        <template #default="{ row }">
          <div class="predicate-cell">
            <ArrowRight class="predicate-arrow" />
            <span class="predicate-text">{{ row.predicate }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="targetName" label="目标实体" min-width="140">
        <template #default="{ row }">
          <span class="entity-link">
            {{ row.targetName }}
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="confidence" label="置信度" width="100" align="center">
        <template #default="{ row }">
          <ScoreTag :score="row.confidence" />
        </template>
      </el-table-column>

      <el-table-column prop="documentName" label="来源文档" min-width="160">
        <template #default="{ row }">
          <span class="doc-name">{{ row.documentName || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
        <template #default="{ row }">
          <span class="time-text">{{ formatTime(row.createdAt) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.relation-table-wrap {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

/* 搜索工具栏 */
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

/* 实体链接（可点击） */
.entity-link {
  font-weight: 600;
  color: var(--color-neon-blue);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.entity-link:hover {
  color: var(--color-primary-light);
  text-decoration: underline;
}

/* 谓词单元格 */
.predicate-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
}

.predicate-arrow {
  color: var(--color-primary);
  flex-shrink: 0;
}

.predicate-text {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  color: var(--text-primary);
  font-size: 13px;
}

/* 文档名称 */
.doc-name {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 时间文字 */
.time-text {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-2);
}
</style>
