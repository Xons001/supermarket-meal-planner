import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  archiveMealPlan,
  changeMealPlanStatus,
  generateMealPlan,
  getMealPlan,
  getMealPlans,
} from '../api/mealPlans'
import type { GenerateMealPlanRequest, MealPlanFilters, MealPlanStatus } from '../types/mealPlan'

export function useMealPlans(filters: MealPlanFilters) {
  return useQuery({
    queryKey: ['meal-plans', filters],
    queryFn: () => getMealPlans(filters),
    placeholderData: keepPreviousData,
  })
}

export function useMealPlan(id?: string) {
  return useQuery({
    queryKey: ['meal-plan', id],
    queryFn: () => getMealPlan(id ?? ''),
    enabled: Boolean(id),
  })
}

export function useGenerateMealPlan() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: GenerateMealPlanRequest) => generateMealPlan(request),
    onSuccess: (plan) => {
      if (plan.mealPlanId) queryClient.setQueryData(['meal-plan', plan.mealPlanId], plan)
      if (plan.persisted) void queryClient.invalidateQueries({ queryKey: ['meal-plans'] })
    },
  })
}

export function useChangeMealPlanStatus(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (status: MealPlanStatus) => changeMealPlanStatus(id, status),
    onSuccess: (plan) => {
      queryClient.setQueryData(['meal-plan', id], plan)
      void queryClient.invalidateQueries({ queryKey: ['meal-plans'] })
    },
  })
}

export function useArchiveMealPlan(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => archiveMealPlan(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['meal-plans'] })
      void queryClient.invalidateQueries({ queryKey: ['meal-plan', id] })
    },
  })
}
