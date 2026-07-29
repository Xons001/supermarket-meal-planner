import type { ProblemDetails } from '../types/api'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
let refreshRequest: Promise<boolean> | null = null

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ProblemDetails

  constructor(status: number, message: string, problem?: ProblemDetails) {
    super(message)
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
