import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  getKgStats,
  getKgEntities,
  getKgRelations,
  getKgEntityDetail,
  getKgGraph,
  searchKgEntities,
} from '@/api/kg'

vi.mock('@/api/request', () => {
  return {
    default: {
      get: vi.fn(),
    },
  }
})

import request from '@/api/request'
const mockedRequest = vi.mocked(request)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/kg', () => {
  it('getKgStats should GET /knowledge-bases/:kbId/kg/stats', async () => {
    const stats = {
      code: 200,
      data: {
        entityCount: 10,
        relationCount: 15,
        avgMentionCount: 3.5,
        topEntities: [],
        typeDistribution: [],
      },
    }
    mockedRequest.get.mockResolvedValue(stats)
    const result = await getKgStats(1)
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1/kg/stats')
    expect(result).toBe(stats)
  })

  it('getKgEntities should GET entities with optional params', async () => {
    const res = { code: 200, data: { content: [], totalElements: 0 } }
    mockedRequest.get.mockResolvedValue(res)
    await getKgEntities(1, { page: 0, size: 20, search: 'test', type: 'PERSON' })
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1/kg/entities', {
      params: { page: 0, size: 20, search: 'test', type: 'PERSON' },
    })
  })

  it('getKgEntities should work without params', async () => {
    mockedRequest.get.mockResolvedValue({ code: 200, data: { content: [], totalElements: 0 } })
    await getKgEntities(1)
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1/kg/entities', { params: undefined })
  })

  it('getKgRelations should GET relations with optional params', async () => {
    const res = { code: 200, data: { content: [], totalElements: 0 } }
    mockedRequest.get.mockResolvedValue(res)
    await getKgRelations(2, { page: 1, size: 10, search: 'rel', minConfidence: 0.5 })
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/2/kg/relations', {
      params: { page: 1, size: 10, search: 'rel', minConfidence: 0.5 },
    })
  })

  it('getKgEntityDetail should GET /entities/:entityId', async () => {
    const res = {
      code: 200,
      data: {
        id: 5,
        name: 'Entity',
        type: 'PERSON',
        description: 'desc',
        mentionCount: 3,
        createdAt: '2024-01-01',
        relations: [],
      },
    }
    mockedRequest.get.mockResolvedValue(res)
    const result = await getKgEntityDetail(1, 5)
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1/kg/entities/5')
    expect(result).toBe(res)
  })

  it('getKgGraph should GET graph visualization data', async () => {
    const res = { code: 200, data: { nodes: [], edges: [] } }
    mockedRequest.get.mockResolvedValue(res)
    await getKgGraph(1, { maxNodes: 50, seedEntityId: 5, maxHops: 2 })
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1/kg/graph', {
      params: { maxNodes: 50, seedEntityId: 5, maxHops: 2 },
    })
  })

  it('getKgGraph should work without params', async () => {
    mockedRequest.get.mockResolvedValue({ code: 200, data: { nodes: [], edges: [] } })
    await getKgGraph(1)
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1/kg/graph', { params: undefined })
  })

  it('searchKgEntities should GET /search with query', async () => {
    const res = { code: 200, data: [] }
    mockedRequest.get.mockResolvedValue(res)
    await searchKgEntities(1, 'entity-name', 10)
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1/kg/search', {
      params: { q: 'entity-name', limit: 10 },
    })
  })

  it('searchKgEntities should work without limit', async () => {
    mockedRequest.get.mockResolvedValue({ code: 200, data: [] })
    await searchKgEntities(1, 'query')
    expect(mockedRequest.get).toHaveBeenCalledWith('/knowledge-bases/1/kg/search', {
      params: { q: 'query', limit: undefined },
    })
  })
})
