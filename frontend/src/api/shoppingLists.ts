import { apiFetch } from './client'
import type {
  ShoppingList,
  ShoppingListFilters,
  ShoppingListPage,
  ShoppingListStatus,
} from '../types/shoppingList'

export function createShoppingList(mealPlanId: string): Promise<ShoppingList> {
  return apiFetch(`/api/v1/meal-plans/${mealPlanId}/shopping-list`, { method: 'POST' })
}

export function getShoppingListForPlan(mealPlanId: string): Promise<ShoppingList> {
  return apiFetch(`/api/v1/meal-plans/${mealPlanId}/shopping-list`)
}

export function regenerateShoppingList(mealPlanId: string): Promise<ShoppingList> {
  return apiFetch(`/api/v1/meal-plans/${mealPlanId}/shopping-list/regenerate`, {
    method: 'POST',
  })
}

export function changeShoppingListStatus(
  mealPlanId: string,
  status: ShoppingListStatus,
): Promise<ShoppingList> {
  return apiFetch(`/api/v1/meal-plans/${mealPlanId}/shopping-list/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export function archiveShoppingList(mealPlanId: string): Promise<void> {
  return apiFetch(`/api/v1/meal-plans/${mealPlanId}/shopping-list`, { method: 'DELETE' })
}

export function getShoppingLists(filters: ShoppingListFilters): Promise<ShoppingListPage> {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      if (key === 'generatedFrom') params.set(key, `${String(value)}T00:00:00Z`)
      else if (key === 'generatedTo') params.set(key, `${String(value)}T23:59:59Z`)
      else params.set(key, String(value))
    }
  })
  return apiFetch(`/api/v1/shopping-lists?${params.toString()}`)
}

export function getShoppingList(id: string): Promise<ShoppingList> {
  return apiFetch(`/api/v1/shopping-lists/${id}`)
}

export const archiveShoppingListById = (id: string) =>
  apiFetch<ShoppingList>(`/api/v1/shopping-lists/${id}/archive`, { method: 'PATCH' })
export const restoreShoppingList = (id: string) =>
  apiFetch<ShoppingList>(`/api/v1/shopping-lists/${id}/restore`, { method: 'PATCH' })
export const activateShoppingList = (id: string) =>
  apiFetch<ShoppingList>(`/api/v1/shopping-lists/${id}/activate`, { method: 'PATCH' })

export function shoppingListCsvUrl(id: string): string {
  return `/api/v1/shopping-lists/${id}/export?format=csv`
}
