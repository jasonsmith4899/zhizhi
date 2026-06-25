<script setup lang="ts">
/**
 * KgOverview — 知识图谱概览主组件
 * 嵌入 KnowledgeDetail.vue 的"知识图谱" tab
 */
import { ref, computed, onMounted, watch } from 'vue'
import {
  Share,
  DataBoard,
  Connection,
  TrendCharts,
  FullScreen,
} from '@element-plus/icons-vue'
import type { KgStats } from '@/api/kg'
import { getKgStats } from '@/api/kg'

import SectionCard from '@/components/ui/SectionCard.vue'
import StatCard from '@/components/ui/StatCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ShineButton from '@/components/ui/ShineButton.vue'
import KgEntityTable from './KgEntityTable.vue'
import KgRelationTable from './KgRelationTable.vue'
import KgEntityDetailDialog from './KgEntityDetailDialog.vue'
import KgGraphDialog from './KgGraphDialog.vue'

const props = defineProps<{ kbId: number }>()

// ---------- 状态 ----------
const loading = ref(false)
const stats = ref<KgStats | null>(null)
const subTab = ref<'entities' | 'relations'>('entities')

// 实体详情弹窗
const detailVisible = ref(false)
const detailEntityId = ref<number | null>(null)

// 图谱可视化弹窗
const graphVisible = ref(false)
const graphSeedId = ref<number | undefined>(undefined)

// ---------- 类型颜色映射 ----------
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
async function loadStats() {
  loading.value = true
  try {
    const res = await getKgStats(props.kbId)
    stats.value = (res as any).data
  } catch {
    // 已由 request 拦截器处理错误提示
  } finally {
    loading.value = false
  }
}

// 平均提及次数（由后端计算，基于全部实体）
const avgMentions = computed(() => {
  return stats.value?.avgMentionCount ?? 0
})

// ---------- 事件处理 ----------
function openEntityDetail(entityId: number) {
  detailEntityId.value = entityId
  detailVisible.value = true
}

function openGraph(seedId?: number) {
  graphSeedId.value = seedId
  graphVisible.value = true
}

onMounted(loadStats)

// 当 kbId 变化时重新加载
watch(() => props.kbId, loadStats)
</script>

<template>
  <div class="kg-overview">
    <!-- 统计卡片行 -->
    <div class="stats-row">
      <StatCard label="实体总数" :value="stats?.entityCount ?? 0" :icon="DataBoard" />
      <StatCard label="关系总数" :value="stats?.relationCount ?? 0" :icon="Connection" />
      <StatCard label="平均提及" :value="avgMentions" :icon="TrendCharts" />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-if="!loading && stats && stats.entityCount === 0"
      title="暂无知识图谱数据"
      subtitle="请先上传文档并完成知识图谱抽取"
      :icon="Share"
      decoration
    />

    <!-- 有数据时展示详情 -->
    <template v-if="stats && stats.entityCount > 0">
      <!-- 实体类型分布 + Top 实体 -->
      <div class="info-grid">
        <!-- 类型分布 -->
        <SectionCard title="实体类型分布" :icon="DataBoard">
          <div class="type-list">
            <div
              v-for="item in stats!.typeDistribution"
              :key="item.type"
              class="type-item"
            >
              <el-tag :type="typeTagType(item.type) as any" effect="dark" size="small">
                {{ item.type }}
              </el-tag>
              <div class="type-bar-wrap">
                <div
                  class="type-bar"
                  :style="{
                    width: `${Math.min(100, (item.count / Math.max(...stats!.typeDistribution.map(t => t.count))) * 100)}%`,
                  }"
                />
              </div>
              <span class="type-count">{{ item.count }}</span>
            </div>
            <div v-if="stats!.typeDistribution.length === 0" class="no-data-text">
              暂无数据
            </div>
          </div>
        </SectionCard>

        <!-- Top 实体 -->
        <SectionCard title="高频实体 Top 10" :icon="TrendCharts">
          <el-table :data="stats!.topEntities" size="small" stripe>
            <el-table-column label="排名" width="60" align="center">
              <template #default="{ $index }">
                <span class="rank-num">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="实体名称" min-width="140" />
            <el-table-column prop="type" label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="typeTagType(row.type) as any" size="small" effect="dark">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="mentionCount" label="提及次数" width="100" align="center">
              <template #default="{ row }">
                <span class="mention-count">{{ row.mentionCount }}</span>
              </template>
            </el-table-column>
          </el-table>
        </SectionCard>
      </div>

      <!-- 工具栏：搜索 + 图谱可视化按钮 -->
      <div class="toolbar">
        <ShineButton size="md" :icon="FullScreen" @click="openGraph()">
          打开图谱可视化
        </ShineButton>
      </div>

      <!-- 子 Tab：实体管理 / 关系管理 -->
      <SectionCard title="数据管理" :icon="Share">
        <template #header-actions>
          <el-tabs v-model="subTab" class="sub-tabs">
            <el-tab-pane label="实体管理" name="entities" />
            <el-tab-pane label="关系管理" name="relations" />
          </el-tabs>
        </template>

        <KgEntityTable
          v-if="subTab === 'entities'"
          :kb-id="kbId"
          @view-detail="openEntityDetail"
          @locate-in-graph="openGraph"
        />
        <KgRelationTable
          v-if="subTab === 'relations'"
          :kb-id="kbId"
          @view-entity="openEntityDetail"
        />
      </SectionCard>
    </template>

    <!-- 实体详情弹窗 -->
    <KgEntityDetailDialog
      v-model:visible="detailVisible"
      :kb-id="kbId"
      :entity-id="detailEntityId"
      @locate-in-graph="openGraph"
    />

    <!-- 图谱可视化弹窗 -->
    <KgGraphDialog
      v-model:visible="graphVisible"
      :kb-id="kbId"
      :seed-entity-id="graphSeedId"
    />
  </div>
</template>

<style scoped>
.kg-overview {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

/* 统计卡片行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

/* 信息网格：类型分布 + Top 实体 */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

/* 类型分布列表 */
.type-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.type-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.type-bar-wrap {
  flex: 1;
  height: 8px;
  background: var(--overlay-stellar-15);
  border-radius: var(--radius-xs);
  overflow: hidden;
}

.type-bar {
  height: 100%;
  background: var(--gradient-decor-line);
  border-radius: var(--radius-xs);
  transition: width var(--transition-normal);
  min-width: 4px;
}

.type-count {
  font-family: 'Orbitron', monospace;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 32px;
  text-align: right;
}

.no-data-text {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: var(--space-4);
}

/* 排名数字 */
.rank-num {
  font-family: 'Orbitron', monospace;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-neon-blue);
}

/* 提及次数 */
.mention-count {
  font-family: 'Orbitron', monospace;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

/* 子 tabs 样式调整 */
.sub-tabs {
  margin: 0;
}

.sub-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.sub-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.sub-tabs :deep(.el-tabs__active-bar) {
  background: var(--color-primary);
}

.sub-tabs :deep(.el-tabs__item) {
  color: var(--text-secondary);
  font-family: 'Rajdhani', sans-serif;
  font-size: 13px;
  height: 32px;
  line-height: 32px;
}

.sub-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-neon-blue);
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-row {
    grid-template-columns: repeat(3, 1fr);
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
