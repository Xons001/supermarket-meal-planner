import type { PageResponse } from './api'

export type CatalogSyncType = 'FULL_CATALOG' | 'PRICES_ONLY'
export type CatalogSyncStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED'
export type CatalogSyncTrigger = 'MANUAL' | 'SCHEDULED' | 'RETRY'

export interface CatalogSyncRun {
  id: string
  supermarketCode: string
  syncType: CatalogSyncType
  triggeredBy: CatalogSyncTrigger
  status: CatalogSyncStatus
  provider: string
  airflowDagId: string | null
  airflowDagRunId: string | null
  retryOfSyncRunId: string | null
  categoriesProcessed: number
  productsProcessed: number
  productsCreated: number
  productsUpdated: number
  productsUnavailable: number
  pricesChanged: number
  validationErrors: number
  result: Record<string, unknown> | null
  requestedAt: string
  startedAt: string | null
  completedAt: string | null
}

export interface CatalogSyncOverview {
  enabled: boolean
  airflowHealthy: boolean
  provider: string
  airflowPublicUrl: string | null
  fullSchedule: string
  priceSchedule: string
  latestRun: CatalogSyncRun | null
}

export interface CatalogSyncError {
  id: string
  severity: 'WARNING' | 'ERROR' | 'FATAL'
  entityType: string
  externalId: string | null
  errorCode: string
  message: string
  createdAt: string
}

export type CatalogSyncPage = PageResponse<CatalogSyncRun>
export type CatalogSyncErrorPage = PageResponse<CatalogSyncError>
