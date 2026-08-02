import { apiFetch } from './client'
import type {
  NutritionCandidatePage,
  NutritionOverview,
  NutritionRunPage,
  NutritionSnapshot,
} from '../types/nutritionAdmin'

export const getNutritionOverview = () =>
  apiFetch<NutritionOverview>('/api/v1/admin/nutrition-enrichment/overview')
export const getNutritionRuns = (page: number) =>
  apiFetch<NutritionRunPage>(`/api/v1/admin/nutrition-enrichment/runs?page=${page}&size=20`)
export const getNutritionCandidates = (page: number) =>
  apiFetch<NutritionCandidatePage>(
    `/api/v1/admin/nutrition-candidates?status=PENDING&page=${page}&size=20`,
  )
export const triggerNutritionEnrichment = (provider: string) =>
  apiFetch<{ runId: string }>('/api/v1/admin/nutrition-enrichment/runs', {
    method: 'POST',
    body: JSON.stringify({ provider }),
  })
export const acceptNutritionCandidate = (id: string, version: number) =>
  apiFetch<NutritionSnapshot>(`/api/v1/admin/nutrition-candidates/${id}/accept`, {
    method: 'POST',
    body: JSON.stringify({
      expectedVersion: version,
      reason: 'Revisado desde el panel de administración',
    }),
  })
export const rejectNutritionCandidate = (id: string, version: number, reason: string) =>
  apiFetch<void>(`/api/v1/admin/nutrition-candidates/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify({ expectedVersion: version, reason }),
  })
