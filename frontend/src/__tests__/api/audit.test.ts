import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getAuditLogs } from '@/api/audit'

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

describe('api/audit', () => {
  it('getAuditLogs should GET /audit-logs with page and size params', async () => {
    const res = { code: 200, data: { content: [], totalElements: 0 } }
    mockedRequest.get.mockResolvedValue(res)
    const result = await getAuditLogs(0, 20)
    expect(mockedRequest.get).toHaveBeenCalledWith('/audit-logs', { params: { page: 0, size: 20 } })
    expect(result).toBe(res)
  })

  it('getAuditLogs should pass different page numbers', async () => {
    mockedRequest.get.mockResolvedValue({ code: 200, data: {} })
    await getAuditLogs(3, 10)
    expect(mockedRequest.get).toHaveBeenCalledWith('/audit-logs', { params: { page: 3, size: 10 } })
  })
})
