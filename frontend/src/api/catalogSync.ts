import { apiFetch } from './client'
import type {
  CatalogSyncErrorPage,
  CatalogSyncOverview,
  CatalogSyncPage,
  CatalogSyncType,
} from '../types/catalogSync'

export const getCatalogSyncOverview = () =>
  apiFetch<CatalogSyncOverview>('/api/v1/admin/catalog-syncs/overview')

export function getCatalogSyncRuns(filters: { page: number; status?: string; syncType?: string }) {
  const params = new URLSearchParams({ page: String(filters.page), size: '20' })
  if (filters.status) params.set('status', filters.status)
  if (filters.syncType) params.set('syncType', filters.syncType)
  return apiFetch<CatalogSyncPage>(`/api/v1/admin/catalog-syncs?${params}`)
}

export const getCatalogSyncErrors = (id: string) =>
  apiFetch<CatalogSyncErrorPage>(`/api/v1/admin/catalog-syncs/${id}/errors?size=100`)

export const triggerCatalogSync = (syncType: CatalogSyncType) =>
  apiFetch<{ syncRunId: string }>('/api/v1/admin/catalog-syncs', {
    method: 'POST',
    body: JSON.stringify({ supermarketCode: 'MERCADONA', syncType }),
  })

export const retryCatalogSync = (id: string) =>
  apiFetch<{ syncRunId: string }>(`/api/v1/admin/catalog-syncs/${id}/retry`, {
    method: 'POST',
  })
