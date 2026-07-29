import { apiFetch, ApiError } from './client'
import type { AuthUser, UserPreferences } from '../types/auth'

export async function getMe(): Promise<AuthUser | null> {
  try {
    return await apiFetch<AuthUser>('/api/v1/auth/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null
    throw error
  }
}
export const register = (body: { email: string; password: string; displayName: string }) =>
  apiFetch<AuthUser>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(body) })
export const login = (body: { email: string; password: string }) =>
  apiFetch<AuthUser>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(body) })
export const logout = () => apiFetch<void>('/api/v1/auth/logout', { method: 'POST' })
export const logoutAll = () => apiFetch<void>('/api/v1/auth/logout-all', { method: 'POST' })
export const updateProfile = (body: { displayName: string }) =>
  apiFetch<AuthUser>('/api/v1/users/me', { method: 'PATCH', body: JSON.stringify(body) })
export const updatePreferences = (body: UserPreferences) =>
  apiFetch<UserPreferences>('/api/v1/users/me/preferences', {
    method: 'PUT',
    body: JSON.stringify(body),
  })
export const changePassword = (body: { currentPassword: string; newPassword: string }) =>
  apiFetch<void>('/api/v1/users/me/change-password', { method: 'POST', body: JSON.stringify(body) })
export const disableAccount = (currentPassword: string) =>
  apiFetch<void>('/api/v1/users/me', {
    method: 'DELETE',
    body: JSON.stringify({ currentPassword }),
  })
