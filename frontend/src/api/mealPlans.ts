import { apiFetch } from './client'
import type {
  GenerateMealPlanRequest,
  GeneratedMealPlan,
  MealPlanFilters,
  MealPlanPage,
  MealPlanStatus,
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
