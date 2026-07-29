import { apiFetch } from './client'
import type {
  GenerateMealPlanRequest,
  GeneratedMealPlan,
  MealPlanFilters,
  MealPlanPage,
  MealPlanStatus,
  AlternativePriority,
  EditPreview,
  MealAlternative,
  MealPlanChangePage,
  MealPlanSummary,
} from '../types/mealPlan'

export function generateMealPlan(request: GenerateMealPlanRequest): Promise<GeneratedMealPlan> {
  return apiFetch('/api/v1/meal-plans/generate', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function getMealPlans(filters: MealPlanFilters): Promise<MealPlanPage> {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  return apiFetch(`/api/v1/meal-plans?${params.toString()}`)
}

export function getMealPlan(id: string): Promise<GeneratedMealPlan> {
  return apiFetch(`/api/v1/meal-plans/${id}`)
}

export function changeMealPlanStatus(
  id: string,
  status: MealPlanStatus,
): Promise<GeneratedMealPlan> {
  return apiFetch(`/api/v1/meal-plans/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export function archiveMealPlan(id: string): Promise<void> {
  return apiFetch(`/api/v1/meal-plans/${id}`, { method: 'DELETE' })
}

export const restoreMealPlan = (id: string) =>
  apiFetch<GeneratedMealPlan>(`/api/v1/meal-plans/${id}/restore`, { method: 'PATCH' })

export const setMealPlanFavorite = (id: string, favorite: boolean) =>
  apiFetch<MealPlanSummary>(`/api/v1/meal-plans/${id}/favorite`, {
    method: 'PATCH',
    body: JSON.stringify({ favorite }),
  })

export const duplicateMealPlan = (id: string, name: string, startDate: string) =>
  apiFetch<GeneratedMealPlan>(`/api/v1/meal-plans/${id}/duplicate`, {
    method: 'POST',
    body: JSON.stringify({ name, startDate }),
  })

export function getMealAlternatives(
  planId: string,
  mealId: string,
  priority: AlternativePriority,
  seed?: number,
): Promise<MealAlternative[]> {
  const params = new URLSearchParams({ priority, limit: '10' })
  if (seed !== undefined) params.set('seed', String(seed))
  return apiFetch(`/api/v1/meal-plans/${planId}/meals/${mealId}/alternatives?${params}`)
}

export function previewMealReplacement(
  planId: string,
  mealId: string,
  mealTemplateId: string,
  expectedEditVersion: number,
  seed?: number,
): Promise<EditPreview> {
  return apiFetch(`/api/v1/meal-plans/${planId}/meals/${mealId}/replacement-previews`, {
    method: 'POST',
    body: JSON.stringify({ mealTemplateId, expectedEditVersion, seed }),
  })
}

export function previewRegeneration(
  planId: string,
  target: 'meals' | 'days',
  targetId: string,
  expectedEditVersion: number,
  seed?: number,
): Promise<EditPreview> {
  return apiFetch(`/api/v1/meal-plans/${planId}/${target}/${targetId}/regeneration-previews`, {
    method: 'POST',
    body: JSON.stringify({ expectedEditVersion, seed }),
  })
}

export function confirmEdit(
  planId: string,
  target: 'meals' | 'days',
  targetId: string,
  action: 'replacements' | 'regenerations',
  previewToken: string,
  expectedEditVersion: number,
): Promise<GeneratedMealPlan> {
  return apiFetch(`/api/v1/meal-plans/${planId}/${target}/${targetId}/${action}`, {
    method: 'POST',
    body: JSON.stringify({ previewToken, expectedEditVersion }),
  })
}

export function setMealLock(
  planId: string,
  mealId: string,
  locked: boolean,
  expectedEditVersion: number,
): Promise<GeneratedMealPlan> {
  return apiFetch(`/api/v1/meal-plans/${planId}/meals/${mealId}/lock`, {
    method: 'PATCH',
    body: JSON.stringify({ locked, expectedEditVersion }),
  })
}

export function undoMealPlanEdit(
  planId: string,
  expectedEditVersion: number,
): Promise<GeneratedMealPlan> {
  return apiFetch(`/api/v1/meal-plans/${planId}/undo`, {
    method: 'POST',
    body: JSON.stringify({ expectedEditVersion }),
  })
}

export function getMealPlanChanges(planId: string): Promise<MealPlanChangePage> {
  return apiFetch(`/api/v1/meal-plans/${planId}/changes?page=0&size=20`)
}
