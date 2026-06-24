<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAuditLogs } from '../../api/audit'
import PageHeader from '../../components/ui/PageHeader.vue'
import GlassCard from '../../components/ui/GlassCard.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'

const loading = ref(false)
const logs = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

onMounted(() => {
  loadLogs()
})

async function loadLogs() {
  loading.value = true
  try {
    const res = await getAuditLogs(currentPage.value - 1, pageSize.value)
    const data = (res as any).data || {}
    logs.value = data.content || []
    total.value = data.totalElements || 0
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadLogs()
}

function formatTime(value: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="操作审计"
      subtitle="追踪租户内的关键操作记录，保障数据可追溯"
      decoration
    />

    <GlassCard padding="lg" radius="lg">
      <div v-loading="loading" class="table-wrap">
        <el-table :data="logs" stripe>
          <el-table-column prop="createdAt" label="时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="action" label="操作" min-width="140" />
          <el-table-column prop="targetType" label="目标类型" min-width="120" />
          <el-table-column prop="targetId" label="目标ID" min-width="100" />
          <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" min-width="130" />
          <el-table-column label="结果" min-width="100">
            <template #default="{ row }">
              <StatusBadge
                :type="row.success ? 'success' : 'danger'"
                :label="row.success ? '成功' : '失败'"
              />
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无审计日志" :image-size="80" />
          </template>
        </el-table>

        <div v-if="total > 0" class="pagination-wrap">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </GlassCard>
  </div>
</template>

<style scoped>
.table-wrap {
  min-height: 200px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-5);
}
</style>
