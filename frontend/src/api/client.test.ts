import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'

describe('authenticated API client', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/'
  })

  it('shares one refresh across concurrent 401 responses and retries each request once', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-value; path=/'
    let protectedCalls = 0
    let refreshCalls = 0
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.includes('/api/v1/auth/refresh')) {
        refreshCalls += 1
        await Promise.resolve()
        return new Response(null, { status: 204 })
      }
      protectedCalls += 1
      if (protectedCalls <= 2) {
        return Response.json({ detail: 'Caducada' }, { status: 401 })
      }
      expect(init?.credentials).toBe('include')
      return Response.json({ ok: true })
    })
    vi.stubGlobal('fetch', fetchMock)

    const [first, second] = await Promise.all([
      apiFetch<{ ok: boolean }>('/api/v1/meal-plans'),
      apiFetch<{ ok: boolean }>('/api/v1/shopping-lists'),
    ])
    expect(first.ok).toBe(true)
    expect(second.ok).toBe(true)
    expect(refreshCalls).toBe(1)
  })

  it('sends CSRF on mutable requests with credentials', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-value; path=/'
    const fetchMock = vi.fn((_input: RequestInfo | URL, init?: RequestInit) => {
      expect(init?.credentials).toBe('include')
      expect(new Headers(init?.headers).get('X-XSRF-TOKEN')).toBe('csrf-value')
      return Promise.resolve(new Response(null, { status: 204 }))
    })
    vi.stubGlobal('fetch', fetchMock)
    await apiFetch<void>('/api/v1/auth/logout', { method: 'POST' })
  })
})
