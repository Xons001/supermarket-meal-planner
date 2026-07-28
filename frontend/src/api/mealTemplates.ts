import { apiFetch } from './client'
import type {
  MealTemplate,
  MealTemplateFilters,
  MealTemplateRequest,
  PageResponse,
} from '../types/api'

export function getMealTemplates(
  filters: MealTemplateFilters,
): Promise<PageResponse<MealTemplate>> {
  const query = new URLSearchParams()
  append(query, 'supermarketCode', filters.supermarketCode)
  append(query, 'mealType', filters.mealType)
  if (filters.active !== undefined) {
    query.set('active', String(filters.active))
  }
  append(query, 'query', filters.query)
  append(query, 'minimumProtein', filters.minimumProtein)
  append(query, 'maximumCalories', filters.maximumCalories)
  append(query, 'maximumPreparationMinutes', filters.maximumPreparationMinutes)
  if (filters.excludedAllergens?.length) {
    query.set('excludedAllergens', filters.excludedAllergens.join(','))
  }
  if (filters.dietaryTags?.length) {
    query.set('dietaryTags', filters.dietaryTags.join(','))
  }
  query.set('page', String(filters.page))
  query.set('size', String(filters.size))
  query.set('sort', filters.sort)
  return apiFetch<PageResponse<MealTemplate>>(`/api/v1/meal-templates?${query}`)
}

export function getMealTemplate(id: string): Promise<MealTemplate> {
  return apiFetch<MealTemplate>(`/api/v1/meal-templates/${id}`)
}

export function createMealTemplate(request: MealTemplateRequest): Promise<MealTemplate> {
  return apiFetch<MealTemplate>('/api/v1/meal-templates', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function updateMealTemplate(
  id: string,
  request: MealTemplateRequest,
): Promise<MealTemplate> {
  return apiFetch<MealTemplate>(`/api/v1/meal-templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function previewMealTemplate(request: MealTemplateRequest): Promise<MealTemplate> {
  return apiFetch<MealTemplate>('/api/v1/meal-templates/preview', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function changeMealTemplateStatus(id: string, active: boolean): Promise<MealTemplate> {
  return apiFetch<MealTemplate>(`/api/v1/meal-templates/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ active }),
  })
}

export function archiveMealTemplate(id: string): Promise<void> {
  return apiFetch<void>(`/api/v1/meal-templates/${id}`, { method: 'DELETE' })
}

function append(query: URLSearchParams, key: string, value: string | undefined): void {
  if (value) {
    query.set(key, value)
  }
}
