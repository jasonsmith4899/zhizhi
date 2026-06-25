<script setup lang="ts">
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { Network, DataSet } from 'vis-network/standalone'
import { getKgGraph, getKgEntityDetail, searchKgEntities } from '../../api/kg'
import type { KgGraph, KgEntity, KgEntityDetail } from '../../api/kg'

const props = defineProps<{
  visible: boolean
  kbId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

// 状态
const loading = ref(false)
const searchQuery = ref('')
const searchResults = ref<KgEntity[]>([])
const selectedEntity = ref<KgEntityDetail | null>(null)
const sidebarVisible = ref(false)
const containerRef = ref<HTMLDivElement>()

let network: Network | null = null
// vis-network 的 DataSet 泛型类型过于严格，这里用 any 承载节点/边对象
let nodesDataset: any = null
let edgesDataset: any = null

// 实体类型颜色映射（深空蓝主题）
const typeColors: Record<string, string> = {
  '人物': '#0066FF',
  '组织': '#7B61FF',
  '产品': '#00D4FF',
  '概念': '#64748b',
}
const defaultColor = '#00ff88'

function getTypeColor(type: string): string {
  return typeColors[type] || defaultColor
}

// 加载图谱数据
async function loadGraph(seedEntityId?: number) {
  loading.value = true
  try {
    const res = await getKgGraph(props.kbId, {
      maxNodes: 200,
      seedEntityId,
      maxHops: seedEntityId ? 2 : undefined
    })
    const data = (res as any).data as KgGraph
    renderGraph(data)
  } catch (e) {
    console.error('加载图谱失败', e)
  } finally {
    loading.value = false
  }
}

// 渲染 vis-network 图谱
function renderGraph(data: KgGraph) {
  if (!containerRef.value) return

  nodesDataset = new DataSet(data.nodes.map(n => ({
    id: n.id,
    label: n.name,
    title: `${n.name}\n类型: ${n.type}\n提及: ${n.mentionCount}`,
    color: {
      background: getTypeColor(n.type),
      border: getTypeColor(n.type),
      highlight: { background: '#00D4FF', border: '#00D4FF' },
      hover: { background: '#00D4FF', border: '#00D4FF' }
    },
    size: Math.min(50, Math.max(15, 15 + n.mentionCount * 2)),
    font: { color: '#e2e8f0', size: 14, face: "'Exo 2', sans-serif" },
    borderWidth: 2,
    shadow: { enabled: true, color: 'rgba(0, 212, 255, 0.3)', size: 10 },
    raw: n
  })))

  edgesDataset = new DataSet(data.edges.map((e, i) => ({
    id: i,
    from: e.source,
    to: e.target,
    label: e.predicate,
    title: `${e.predicate} (${Math.round(e.confidence * 100)}%)`,
    arrows: { to: { enabled: true, scaleFactor: 0.5 } },
    color: { color: 'rgba(0, 212, 255, 0.3)', highlight: '#00D4FF', hover: '#00D4FF' },
    font: { color: '#94a3b8', size: 11, strokeWidth: 0 },
    width: Math.max(1, Math.min(3, 1 + e.confidence * 2)),
    smooth: { type: 'continuous' }
  })))

  const options: any = {
    nodes: {
      shape: 'dot',
      font: { color: '#e2e8f0', size: 14, face: "'Exo 2', sans-serif" }
    },
    edges: {
      smooth: { type: 'continuous' }
    },
    physics: {
      solver: 'forceAtlas2Based' as const,
      forceAtlas2Based: {
        gravitationalConstant: -50,
        centralGravity: 0.01,
        springLength: 150,
        springConstant: 0.08
      },
      stabilization: { iterations: 150 }
    },
    interaction: {
      hover: true,
      tooltipDelay: 200,
      navigationButtons: true,
      keyboard: true
    }
  }

  network = new Network(containerRef.value, { nodes: nodesDataset, edges: edgesDataset } as any, options)

  // 点击节点：高亮关联 + 显示详情
  network.on('click', async (params: any) => {
    if (params.nodes.length > 0) {
      const nodeId = params.nodes[0]
      highlightConnected(nodeId)
      await loadEntityDetail(nodeId)
    } else {
      resetHighlight()
      sidebarVisible.value = false
      selectedEntity.value = null
    }
  })

  // 双击节点：展开二跳邻居
  network.on('doubleClick', (params: any) => {
    if (params.nodes.length > 0) {
      const nodeId = params.nodes[0]
      loadGraph(nodeId)
    }
  })
}

// 高亮节点及其直接关联
function highlightConnected(nodeId: number) {
  if (!network || !nodesDataset || !edgesDataset) return
  const connectedNodes = network.getConnectedNodes(nodeId) as number[]
  const allNodeIds = nodesDataset.getIds() as number[]
  const connectedEdges = network.getConnectedEdges(nodeId) as number[]

  // 半透明化非关联节点
  allNodeIds.forEach(id => {
    if (id !== nodeId && !connectedNodes.includes(id)) {
      nodesDataset!.update({ id, opacity: 0.15 })
    } else {
      nodesDataset!.update({ id, opacity: 1 })
    }
  })

  // 半透明化非关联边
  const allEdgeIds = edgesDataset.getIds() as number[]
  allEdgeIds.forEach(id => {
    if (!connectedEdges.includes(id)) {
      edgesDataset!.update({ id, opacity: 0.1 })
    } else {
      edgesDataset!.update({ id, opacity: 1 })
    }
  })
}

// 重置高亮
function resetHighlight() {
  if (!nodesDataset || !edgesDataset) return
  nodesDataset.getIds().forEach((id: any) => nodesDataset!.update({ id, opacity: 1 }))
  edgesDataset.getIds().forEach((id: any) => edgesDataset!.update({ id, opacity: 1 }))
}

// 加载实体详情
async function loadEntityDetail(entityId: number) {
  try {
    const res = await getKgEntityDetail(props.kbId, entityId)
    selectedEntity.value = (res as any).data as KgEntityDetail
    sidebarVisible.value = true
  } catch (e) {
    console.error('加载实体详情失败', e)
  }
}

// 搜索实体
async function handleSearch() {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }
  try {
    const res = await searchKgEntities(props.kbId, searchQuery.value, 10)
    searchResults.value = (res as any).data as KgEntity[]
  } catch (e) {
    console.error('搜索失败', e)
  }
}

// 定位到搜索结果
function focusNode(entity: KgEntity) {
  if (!network) return
  network.focus(entity.id, { scale: 1.5, animation: true })
  network.selectNodes([entity.id])
  highlightConnected(entity.id)
  loadEntityDetail(entity.id)
  searchResults.value = []
  searchQuery.value = ''
}

// 监听 dialog 打开
watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => loadGraph())
  } else {
    if (network) {
      network.destroy()
      network = null
    }
    sidebarVisible.value = false
    selectedEntity.value = null
  }
})

onBeforeUnmount(() => {
  if (network) network.destroy()
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title=""
    fullscreen
    :show-close="false"
    class="kg-graph-dialog"
    @update:model-value="emit('update:visible', $event)"
  >
    <!-- 顶部工具栏 -->
    <div class="graph-toolbar">
      <div class="graph-title">
        <span class="graph-icon">🌐</span>
        <span>图谱可视化</span>
      </div>
      <div class="graph-controls">
        <div class="search-wrapper">
          <el-input
            v-model="searchQuery"
            placeholder="搜索实体..."
            clearable
            @input="handleSearch"
            @clear="searchResults = []"
          />
          <div v-if="searchResults.length" class="search-dropdown">
            <div
              v-for="entity in searchResults"
              :key="entity.id"
              class="search-item"
              @click="focusNode(entity)"
            >
              <span class="search-item-name">{{ entity.name }}</span>
              <el-tag size="small" :style="{ background: getTypeColor(entity.type), border: 'none', color: '#fff' }">
                {{ entity.type }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-button @click="emit('update:visible', false)">关闭</el-button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="graph-body">
      <!-- vis-network 画布 -->
      <div ref="containerRef" v-loading="loading" class="graph-canvas" />

      <!-- 实体详情侧边栏 -->
      <transition name="slide-right">
        <div v-if="sidebarVisible && selectedEntity" class="entity-sidebar">
          <div class="sidebar-header">
            <span class="sidebar-title">实体详情</span>
            <el-button text @click="sidebarVisible = false">✕</el-button>
          </div>
          <div class="sidebar-content">
            <div class="entity-info">
              <div class="entity-name">{{ selectedEntity.name }}</div>
              <el-tag :style="{ background: getTypeColor(selectedEntity.type), border: 'none', color: '#fff' }">
                {{ selectedEntity.type }}
              </el-tag>
            </div>
            <div v-if="selectedEntity.description" class="entity-desc">
              {{ selectedEntity.description }}
            </div>
            <div class="entity-meta">
              <span>提及次数: {{ selectedEntity.mentionCount }}</span>
            </div>
            <div class="sidebar-section-title">关联关系</div>
            <div class="relation-list">
              <div
                v-for="rel in selectedEntity.relations"
                :key="rel.id"
                class="relation-item"
              >
                <span v-if="rel.direction === 'out'" class="relation-arrow">
                  → {{ rel.otherEntity }}
                </span>
                <span v-else class="relation-arrow">
                  ← {{ rel.otherEntity }}
                </span>
                <span class="relation-predicate">{{ rel.predicate }}</span>
                <span class="relation-confidence">{{ Math.round(rel.confidence * 100) }}%</span>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </el-dialog>
</template>

<style scoped>
.kg-graph-dialog {
  --graph-bg: var(--bg-dark);
}

.kg-graph-dialog :deep(.el-dialog) {
  background: var(--graph-bg);
  border: 1px solid var(--border-color);
}

.kg-graph-dialog :deep(.el-dialog__header) {
  display: none;
}

.kg-graph-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 工具栏 */
.graph-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-5);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  z-index: var(--z-sticky);
}

.graph-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-family: 'Rajdhani', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.graph-icon {
  font-size: 20px;
}

.graph-controls {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.search-wrapper {
  position: relative;
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  margin-top: var(--space-1);
  max-height: 300px;
  overflow-y: auto;
  z-index: var(--z-overlay);
  backdrop-filter: blur(var(--blur-glass));
}

.search-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-2) var(--space-3);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.search-item:hover {
  background: var(--overlay-primary-10);
}

.search-item-name {
  color: var(--text-primary);
  font-size: 14px;
}

/* 主体 */
.graph-body {
  flex: 1;
  display: flex;
  position: relative;
  overflow: hidden;
}

.graph-canvas {
  flex: 1;
  background: var(--graph-bg);
}

/* 侧边栏 */
.entity-sidebar {
  width: 320px;
  background: var(--bg-card);
  border-left: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  backdrop-filter: blur(var(--blur-glass));
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.sidebar-title {
  font-family: 'Rajdhani', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.sidebar-content {
  padding: var(--space-4);
  flex: 1;
  overflow-y: auto;
}

.entity-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.entity-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.entity-desc {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: var(--space-3);
}

.entity-meta {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: var(--space-4);
}

.sidebar-section-title {
  font-family: 'Rajdhani', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: var(--space-3);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--border-color);
}

.relation-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.relation-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: var(--overlay-primary-05);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

.relation-arrow {
  color: var(--text-primary);
  font-size: 13px;
  flex: 1;
}

.relation-predicate {
  color: var(--color-neon-blue);
  font-size: 12px;
  font-weight: 500;
}

.relation-confidence {
  color: var(--text-muted);
  font-size: 12px;
  font-family: 'Orbitron', monospace;
}

/* 侧边栏滑入动画 */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform var(--transition-normal);
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}
</style>
