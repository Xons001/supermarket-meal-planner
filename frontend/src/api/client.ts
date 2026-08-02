import type { ProblemDetails } from '../types/api'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
let refreshRequest: Promise<boolean> | null = null

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ProblemDetails

  constructor(status: number, message: string, problem?: ProblemDetails) {
    super(problem?.correlationId ? `${message} (referencia: ${problem.correlationId})` : message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  return request<T>(path, init, true)
}

async function request<T>(
  path: string,
  init: RequestInit | undefined,
  mayRefresh: boolean,
): Promise<T> {
  const method = (init?.method ?? 'GET').toUpperCase()
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    await ensureCsrf()
  }
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...(!['GET', 'HEAD', 'OPTIONS'].includes(method)
        ? { 'X-XSRF-TOKEN': csrfCookie() ?? '' }
        : {}),
      'X-Request-ID': createRequestId(),
      ...init?.headers,
    },
  })

  if (response.status === 401 && mayRefresh && !refreshExcluded(path)) {
    const refreshed = await refreshOnce()
    if (refreshed) return request<T>(path, init, false)
    if (path !== '/api/v1/auth/me') {
      window.dispatchEvent(new Event('smp:session-expired'))
    }
  }
  if (!response.ok) {
    const problem = await readProblemDetails(response)
    throw new ApiError(
      response.status,
      problem?.detail ?? `API request failed with status ${response.status}`,
      problem,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export async function downloadUserExport(): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/v1/users/me/export`, {
    credentials: 'include',
    headers: { Accept: 'application/json', 'X-Request-ID': createRequestId() },
  })
  if (!response.ok) {
    const problem = await readProblemDetails(response)
    throw new ApiError(response.status, problem?.detail ?? 'No se pudo exportar', problem)
  }
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `supermarket-meal-planner-export-${new Date().toISOString().slice(0, 10)}.json`
  link.click()
  URL.revokeObjectURL(url)
}

function refreshExcluded(path: string): boolean {
  return [
    '/api/v1/auth/csrf',
    '/api/v1/auth/register',
    '/api/v1/auth/login',
    '/api/v1/auth/refresh',
    '/api/v1/auth/logout',
    '/api/v1/auth/logout-all',
  ].includes(path)
}

export async function ensureCsrf(): Promise<void> {
  if (csrfCookie()) return
  await fetch(`${apiBaseUrl}/api/v1/auth/csrf`, { credentials: 'include' })
}

function csrfCookie(): string | undefined {
  return document.cookie
    .split('; ')
    .find((value) => value.startsWith('XSRF-TOKEN='))
    ?.slice('XSRF-TOKEN='.length)
}

function createRequestId(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = ((bytes[6] ?? 0) & 0x0f) | 0x40
  bytes[8] = ((bytes[8] ?? 0) & 0x3f) | 0x80
  const value = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${value.slice(0, 8)}-${value.slice(8, 12)}-${value.slice(12, 16)}-${value.slice(16, 20)}-${value.slice(20)}`
}

async function refreshOnce(): Promise<boolean> {
  if (!refreshRequest) {
    refreshRequest = (async () => {
      await ensureCsrf()
      const response = await fetch(`${apiBaseUrl}/api/v1/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'X-XSRF-TOKEN': csrfCookie() ?? '' },
      })
      return response.ok
    })().finally(() => {
      refreshRequest = null
    })
  }
  return refreshRequest
}

async function readProblemDetails(response: Response): Promise<ProblemDetails | undefined> {
  try {
    return (await response.json()) as ProblemDetails
  } catch {
    return undefined
  }
}
