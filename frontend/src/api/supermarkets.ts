import { apiFetch } from './client'
import type { Supermarket } from '../types/api'

export function getSupermarkets(): Promise<Supermarket[]> {
  return apiFetch<Supermarket[]>('/api/v1/supermarkets')
}
