import type { PageResponse } from './api'

export type NutritionRunStatus =
  'PENDING' | 'RUNNING' | 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED' | 'CANCELLED'
export interface NutritionRun {
  id: string
  provider: string
  status: NutritionRunStatus
  triggeredBy: 'MANUAL' | 'SCHEDULED' | 'RETRY'
  startedAt: string | null
  finishedAt: string | null
  productsScanned: number
  barcodeMatches: number
  nameMatches: number
  autoAccepted: number
  pendingReview: number
  rejected: number
  updatedProducts: number
  unchangedProducts: number
  errors: number
  durationMs: number | null
  createdAt: string
}
export interface NutritionOverview {
  enabled: boolean
  provider: string
  cron: string
  productsWithoutNutrition: number
  partialProducts: number
  verifiedProducts: number
  pendingCandidates: number
  latestRun: NutritionRun | null
}
export interface NutritionSnapshot {
  basis: string
  calories: number | null
  protein: number | null
  carbohydrates: number | null
  fat: number | null
  fiber: number | null
  sugars: number | null
  salt: number | null
  saturatedFat: number | null
  dataSource: string
  verificationStatus: string
  confidenceScore: number
  completeness: string
  sourceReference: string | null
  updatedAt: string
}
export interface NutritionCandidate {
  id: string
  productId: string
  productName: string
  currentSource: string | null
  provider: string
  externalReference: string
  externalName: string
  brand: string | null
  matchMethod: string
  confidenceScore: number
  scoreBreakdown: Record<string, unknown>
  status: string
  rejectionReason: string | null
  externalNutrition: Record<string, number | string | null>
  currentNutrition: NutritionSnapshot | null
  expiresAt: string
  reviewedAt: string | null
  version: number
}
export type NutritionRunPage = PageResponse<NutritionRun>
export type NutritionCandidatePage = PageResponse<NutritionCandidate>
