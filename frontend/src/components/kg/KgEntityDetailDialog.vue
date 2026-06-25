<script setup lang="ts">
/**
 * KgEntityDetailDialog — 实体详情弹窗
 * 展示实体基本信息和关联关系
 */
import { ref, watch, computed } from 'vue'
import { Position } from '@element-plus/icons-vue'
import type { KgEntityDetail } from '@/api/kg'
import { getKgEntityDetail } from '@/api/kg'
import ScoreTag from '@/components/ui/ScoreTag.vue'
import ShineButton from '@/components/ui/ShineButton.vue'

const props = defineProps<{
  kbId: number
  entityId: number | null
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  locateInGraph: [entityId: number]
}>()

// ---------- 状态 ----------
const loading = ref(false)
const detail = ref<KgEntityDetail | null>(null)

// v-model 控制弹窗
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

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
async function loadDetail() {
  if (!props.entityId) return
  loading.value = true
  detail.value = null
  try {
    const res = await getKgEntityDetail(props.kbId, props.entityId)
    detail.value = (res as any).data
  } catch {
    // 已由拦截器处理
  } finally {
    loading.value = false
  }
}

// ---------- 事件处理 ----------
function handleLocate() {
  if (detail.value) {
    emit('locateInGraph', detail.value.id)
    dialogVisible.value = false
  }
}

// 格式化时间
function formatTime(val: string): string {
  if (!val) return '-'
  return new Date(val).toLocaleString('zh-CN')
}

// 监听弹窗打开
watch(() => props.visible, (val) => {
  if (val && props.entityId) {
    loadDetail()
  }
})
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="实体详情"
    width="680px"
    top="8vh"
    destroy-on-close
    class="kg-detail-dialog"
  >
    <div v-loading="loading" class="detail-body">
      <template v-if="detail">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border class="info-desc">
          <el-descriptions-item label="实体名称">
            <span class="entity-name">{{ detail.name }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="typeTagType(detail.type) as any" effect="dark" size="small">
              {{ detail.type }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ detail.description || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="提及次数">
            <span class="mention-num">{{ detail.mentionCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatTime(detail.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 关联关系 -->
        <div class="relations-section">
          <h4 class="section-subtitle">关联关系 ({{ detail.relations.length }})</h4>
          <el-table :data="detail.relations" size="small" stripe max-height="300">
            <el-table-column label="方向" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.direction === 'out' ? 'success' : 'warning'" size="small">
                  {{ row.direction === 'out' ? '出' : '入' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="predicate" label="谓词" min-width="120">
              <template #default="{ row }">
                <span class="predicate-text">{{ row.predicate }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="otherEntity" label="关联实体" min-width="140">
              <template #default="{ row }">
                <span class="other-entity">{{ row.otherEntity }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="confidence" label="置信度" width="100" align="center">
              <template #default="{ row }">
                <ScoreTag :score="row.confidence" />
              </template>
            </el-table-column>
          </el-table>
          <div v-if="detail.relations.length === 0" class="no-relations">
            暂无关联关系
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <ShineButton
          v-if="detail"
          size="md"
          :icon="Position"
          @click="handleLocate"
        >
          在图谱中定位
        </ShineButton>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.detail-body {
  min-height: 200px;
}

/* 实体名称 */
.entity-name {
  font-family: 'Rajdhani', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-neon-blue);
}

/* 提及次数 */
.mention-num {
  font-family: 'Orbitron', monospace;
  font-weight: 600;
  color: var(--text-primary);
}

/* 关联关系区域 */
.relations-section {
  margin-top: var(--space-5);
}

.section-subtitle {
  font-family: 'Rajdhani', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-3);
}

/* 谓词文字 */
.predicate-text {
  font-family: 'Rajdhani', sans-serif;
  font-weight: 600;
  color: var(--text-primary);
}

/* 关联实体 */
.other-entity {
  color: var(--color-neon-blue);
  font-weight: 500;
}

/* 无关系提示 */
.no-relations {
  text-align: center;
  color: var(--text-muted);
  padding: var(--space-6);
  font-size: 13px;
}

/* 弹窗底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}
</style>
