import { apiFetch } from './client'
import type { CatalogOption, Category } from '../types/api'

export function getCategories(supermarketCode?: string): Promise<Category[]> {
  const suffix = supermarketCode ? `?${new URLSearchParams({ supermarketCode }).toString()}` : ''
  return apiFetch<Category[]>(`/api/v1/categories${suffix}`)
}

export function getDietaryTags(): Promise<CatalogOption[]> {
  return apiFetch<CatalogOption[]>('/api/v1/dietary-tags')
}

export function getAllergens(): Promise<CatalogOption[]> {
  return apiFetch<CatalogOption[]>('/api/v1/allergens')
}
