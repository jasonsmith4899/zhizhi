<script setup lang="ts">
/**
 * KgEntityTable — 实体管理表格
 * 支持分页、搜索、类型筛选
 */
import { ref, watch } from 'vue'
import { Search, Refresh, View, Position } from '@element-plus/icons-vue'
import type { KgEntity } from '@/api/kg'
import { getKgEntities } from '@/api/kg'

const props = defineProps<{ kbId: number }>()

const emit = defineEmits<{
  viewDetail: [entityId: number]
  locateInGraph: [entityId: number]
}>()

// ---------- 状态 ----------
const loading = ref(false)
const entities = ref<KgEntity[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = ref('')
const typeFilter = ref('')

// 类型选项（常见实体类型）
const typeOptions = [
  { label: '全部类型', value: '' },
  { label: '人物', value: '人物' },
  { label: '组织', value: '组织' },
  { label: '产品', value: '产品' },
  { label: '概念', value: '概念' },
  { label: '地点', value: '地点' },
  { label: '事件', value: '事件' },
  { label: '技术', value: '技术' },
]

// 类型颜色映射
const typeColorMap: Record<string, string> = {
  '人物': '',
  '组织': 'info',
  '产品': '',
  '概念': 'info',
}

function typeTagType(type: string): string {
  return typeColorMap[type] ?? ''
}

// ---------- 数据加载 ----------
async function loadEntities() {
  loading.value = true
  try {
    const res = await getKgEntities(props.kbId, {
      page: page.value - 1, // 后端分页从 0 开始
      size: size.value,
      search: search.value || undefined,
      type: typeFilter.value || undefined,
    })
    const data = (res as any).data
    entities.value = data.content
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
  loadEntities()
}

function handleRefresh() {
  search.value = ''
  typeFilter.value = ''
  page.value = 1
  loadEntities()
}

function handlePageChange(newPage: number) {
  page.value = newPage
  loadEntities()
}

function handleSizeChange(newSize: number) {
  size.value = newSize
  page.value = 1
  loadEntities()
}

// 格式化时间
function formatTime(val: string): string {
  if (!val) return '-'
  return new Date(val).toLocaleString('zh-CN')
}

// 监听 kbId 变化
watch(() => props.kbId, () => {
  page.value = 1
  loadEntities()
}, { immediate: true })
</script>

<template>
  <div class="entity-table-wrap">
    <!-- 搜索工具栏 -->
    <div class="table-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="search"
          placeholder="搜索实体名称..."
          clearable
          :prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="typeFilter"
          placeholder="类型筛选"
          clearable
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option
            v-for="opt in typeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>
      <el-button :icon="Refresh" circle @click="handleRefresh" />
    </div>

    <!-- 实体表格 -->
    <el-table :data="entities" v-loading="loading" stripe>
      <el-table-column prop="name" label="实体名称" min-width="160">
        <template #default="{ row }">
          <span class="entity-name">{{ row.name }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="type" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="typeTagType(row.type) as any" size="small" effect="dark">
            {{ row.type }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="description" label="描述" min-width="200">
        <template #default="{ row }">
          <span class="ellipsis-text">{{ row.description || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="mentionCount" label="提及次数" width="100" align="center">
        <template #default="{ row }">
          <span class="mention-num">{{ row.mentionCount }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
        <template #default="{ row }">
          <span class="time-text">{{ formatTime(row.createdAt) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="View" @click="emit('viewDetail', row.id)">
            详情
          </el-button>
          <el-button type="primary" link :icon="Position" @click="emit('locateInGraph', row.id)">
            图谱定位
          </el-button>
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
.entity-table-wrap {
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

/* 实体名称 */
.entity-name {
  font-weight: 600;
  color: var(--text-primary);
}

/* 文本溢出省略 */
.ellipsis-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
}

/* 提及次数 */
.mention-num {
  font-family: 'Orbitron', monospace;
  font-weight: 600;
  color: var(--color-neon-blue);
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
