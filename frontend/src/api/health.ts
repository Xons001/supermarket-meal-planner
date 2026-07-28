import { apiFetch } from './client'
import type { HealthResponse } from '../types/api'

export function getBackendHealth(): Promise<HealthResponse> {
  return apiFetch<HealthResponse>('/actuator/health')
}
