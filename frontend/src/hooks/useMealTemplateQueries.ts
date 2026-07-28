import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  archiveMealTemplate,
  changeMealTemplateStatus,
  createMealTemplate,
  getMealTemplate,
  getMealTemplates,
  previewMealTemplate,
  updateMealTemplate,
} from '../api/mealTemplates'
import type { MealTemplateFilters, MealTemplateRequest } from '../types/api'

export function useMealTemplates(filters: MealTemplateFilters) {
  return useQuery({
    queryKey: ['meal-templates', filters],
    queryFn: () => getMealTemplates(filters),
    placeholderData: keepPreviousData,
  })
}

export function useMealTemplate(id?: string) {
  return useQuery({
    queryKey: ['meal-template', id],
    queryFn: () => getMealTemplate(id ?? ''),
    enabled: Boolean(id),
  })
}

export function useCreateMealTemplate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createMealTemplate,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['meal-templates'] }),
  })
}

export function useUpdateMealTemplate(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: MealTemplateRequest) => updateMealTemplate(id, request),
    onSuccess: (template) => {
      queryClient.setQueryData(['meal-template', id], template)
      void queryClient.invalidateQueries({ queryKey: ['meal-templates'] })
    },
  })
}

export function usePreviewMealTemplate() {
  return useMutation({ mutationFn: previewMealTemplate })
}

export function useChangeMealTemplateStatus(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (active: boolean) => changeMealTemplateStatus(id, active),
    onSuccess: (template) => {
      queryClient.setQueryData(['meal-template', id], template)
      void queryClient.invalidateQueries({ queryKey: ['meal-templates'] })
    },
  })
}

export function useArchiveMealTemplate(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => archiveMealTemplate(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['meal-templates'] }),
  })
}
