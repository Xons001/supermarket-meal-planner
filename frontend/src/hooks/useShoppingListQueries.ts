import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  archiveShoppingList,
  createShoppingList,
  getShoppingList,
  getShoppingListForPlan,
  getShoppingLists,
  regenerateShoppingList,
} from '../api/shoppingLists'
import type { ShoppingListFilters } from '../types/shoppingList'

export function useShoppingListForPlan(mealPlanId: string) {
  return useQuery({
    queryKey: ['shopping-list-for-plan', mealPlanId],
    queryFn: () => getShoppingListForPlan(mealPlanId),
    enabled: Boolean(mealPlanId),
    retry: false,
  })
}

export function useShoppingList(id: string) {
  return useQuery({
    queryKey: ['shopping-list', id],
    queryFn: () => getShoppingList(id),
    enabled: Boolean(id),
  })
}

export function useShoppingLists(filters: ShoppingListFilters) {
  return useQuery({
    queryKey: ['shopping-lists', filters],
    queryFn: () => getShoppingLists(filters),
    placeholderData: keepPreviousData,
  })
}

export function useCreateShoppingList(mealPlanId: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => createShoppingList(mealPlanId),
    onSuccess: (list) => {
      client.setQueryData(['shopping-list-for-plan', mealPlanId], list)
      client.setQueryData(['shopping-list', list.id], list)
      void client.invalidateQueries({ queryKey: ['shopping-lists'] })
    },
  })
}

export function useRegenerateShoppingList(mealPlanId: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => regenerateShoppingList(mealPlanId),
    onSuccess: (list) => {
      client.setQueryData(['shopping-list-for-plan', mealPlanId], list)
      client.setQueryData(['shopping-list', list.id], list)
      void client.invalidateQueries({ queryKey: ['shopping-lists'] })
    },
  })
}

export function useArchiveShoppingList(mealPlanId: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => archiveShoppingList(mealPlanId),
    onSuccess: () => {
      client.removeQueries({ queryKey: ['shopping-list-for-plan', mealPlanId] })
      void client.invalidateQueries({ queryKey: ['shopping-lists'] })
    },
  })
}
