import request from './request'

export function getAuditLogs(page: number, size: number) {
  return request.get('/audit-logs', { params: { page, size } })
}
