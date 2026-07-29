import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { AuthProvider } from './AuthProvider'
import { ProtectedRoute } from './ProtectedRoute'

function LoginDestination() {
  const location = useLocation()
  const state = location.state as { from?: string } | null
  return <output>{state?.from}</output>
}

describe('ProtectedRoute', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('keeps only a safe internal destination when redirecting to login', async () => {
    render(
      <QueryClientProvider client={createQueryClient()}>
        <AuthProvider initialUser={null}>
          <MemoryRouter initialEntries={['/meal-plans/123?tab=history']}>
            <Routes>
              <Route
                path="/meal-plans/:id"
                element={
                  <ProtectedRoute>
                    <h1>Privado</h1>
                  </ProtectedRoute>
                }
              />
              <Route path="/login" element={<LoginDestination />} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>,
    )
    expect(await screen.findByText('/meal-plans/123?tab=history')).toBeInTheDocument()
  })

  it('finishes session restoration and redirects when refresh is unavailable', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((input: RequestInfo | URL) => {
        const url = String(input)
        if (url.endsWith('/api/v1/auth/csrf')) {
          return Promise.resolve(new Response(null, { status: 204 }))
        }
        return Promise.resolve(Response.json({ detail: 'Sin sesión' }, { status: 401 }))
      }),
    )

    render(
      <QueryClientProvider client={createQueryClient()}>
        <AuthProvider>
          <MemoryRouter initialEntries={['/meal-plans']}>
            <Routes>
              <Route
                path="/meal-plans"
                element={
                  <ProtectedRoute>
                    <h1>Privado</h1>
                  </ProtectedRoute>
                }
              />
              <Route path="/login" element={<h1>Inicia sesión</h1>} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(screen.queryByText('Restaurando sesión…')).not.toBeInTheDocument()
  })
})
