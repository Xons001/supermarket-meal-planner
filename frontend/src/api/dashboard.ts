import { apiFetch } from './client'
import type { ActivityPage, Dashboard } from '../types/dashboard'

export const getDashboard = () => apiFetch<Dashboard>('/api/v1/dashboard')

export function getActivity(type: string, page: number, size = 20) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (type) params.set('type', type)
  return apiFetch<ActivityPage>(`/api/v1/activity?${params}`)
}
