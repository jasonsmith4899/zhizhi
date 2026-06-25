import request from './request'
import type { ApiResponse } from '../types/api'

// ==================== 类型定义 ====================

/** 图谱统计概览 */
export interface KgStats {
  entityCount: number
  relationCount: number
  avgMentionCount: number
  topEntities: Array<{ name: string; type: string; mentionCount: number }>
  typeDistribution: Array<{ type: string; count: number }>
}

/** 实体列表项 */
export interface KgEntity {
  id: number
  name: string
  type: string
  description: string
  mentionCount: number
  createdAt: string
}

/** 关系列表项 */
export interface KgRelation {
  id: number
  sourceName: string
  targetName: string
  predicate: string
  confidence: number
  documentId: number
  documentName: string
  createdAt: string
}

/** 实体详情（含关联关系） */
export interface KgEntityDetail extends KgEntity {
  relations: Array<{
    id: number
    predicate: string
    direction: 'in' | 'out'
    otherEntity: string
    confidence: number
  }>
}

/** 图谱可视化数据 */
export interface KgGraph {
  nodes: Array<{ id: number; name: string; type: string; mentionCount: number }>
  edges: Array<{ source: number; target: number; predicate: string; confidence: number }>
}

// ==================== API 函数 ====================

const BASE = '/knowledge-bases'

/** 获取知识图谱统计概览 */
export function getKgStats(kbId: number) {
  return request.get(`${BASE}/${kbId}/kg/stats`) as Promise<ApiResponse<KgStats>>
}

/** 分页查询实体列表 */
export function getKgEntities(kbId: number, params?: {
  page?: number
  size?: number
  search?: string
  type?: string
  sortBy?: string
  sortOrder?: string
}) {
  return request.get(`${BASE}/${kbId}/kg/entities`, { params }) as Promise<ApiResponse<{ content: KgEntity[]; totalElements: number }>>
}

/** 分页查询关系列表 */
export function getKgRelations(kbId: number, params?: {
  page?: number
  size?: number
  search?: string
  minConfidence?: number
}) {
  return request.get(`${BASE}/${kbId}/kg/relations`, { params }) as Promise<ApiResponse<{ content: KgRelation[]; totalElements: number }>>
}

/** 获取实体详情（含关联关系） */
export function getKgEntityDetail(kbId: number, entityId: number) {
  return request.get(`${BASE}/${kbId}/kg/entities/${entityId}`) as Promise<ApiResponse<KgEntityDetail>>
}

/** 获取图谱可视化数据（节点 + 边） */
export function getKgGraph(kbId: number, params?: {
  maxNodes?: number
  seedEntityId?: number
  maxHops?: number
}) {
  return request.get(`${BASE}/${kbId}/kg/graph`, { params }) as Promise<ApiResponse<KgGraph>>
}

/** 搜索实体（自动补全 / 模糊搜索） */
export function searchKgEntities(kbId: number, q: string, limit?: number) {
  return request.get(`${BASE}/${kbId}/kg/search`, { params: { q, limit } }) as Promise<ApiResponse<KgEntity[]>>
}
