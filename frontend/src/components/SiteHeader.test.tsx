import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { AuthProvider } from '../auth/AuthProvider'
import { useAuth } from '../auth/AuthProvider'
import type { AuthUser } from '../types/auth'
import { SiteHeader } from './SiteHeader'

const user: AuthUser = {
  id: '00000000-0000-4000-8000-000000000701',
  email: 'usuario@example.test',
  displayName: 'Sean',
  status: 'ACTIVE',
  role: 'USER',
  createdAt: '2026-07-29T00:00:00Z',
  preferences: {
    dailyCaloriesTarget: 2000,
    dailyProteinTarget: 100,
    weeklyBudget: 70,
    numberOfDays: 7,
    mealsPerDay: 4,
    strategy: 'PURCHASE_AWARE_SCORING',
    optimizationPreset: 'BALANCED',
    dietaryRestrictions: [],
    allergens: [],
    theme: 'SYSTEM',
  },
}

function renderHeader(identity: AuthUser | null) {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <AuthProvider initialUser={identity}>
        <MemoryRouter>
          <SiteHeader />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

function IdentityHarness() {
  const { setIdentity } = useAuth()
  return (
    <>
      <button type="button" onClick={() => setIdentity(user)}>
        Simular acceso
      </button>
      <SiteHeader />
    </>
  )
}

describe('SiteHeader', () => {
  it('shows public discovery and clear authentication actions', () => {
    renderHeader(null)

    expect(screen.getByRole('link', { name: 'Catálogo' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /funciona/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Mis planes' })).not.toBeInTheDocument()
  })

  it('replaces authentication actions with private navigation, profile and logout', () => {
    renderHeader(user)

    expect(screen.getByRole('link', { name: 'Mis planes' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Mis listas' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByLabelText(/Abrir.*cuenta/)).toHaveTextContent('Sean')
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Iniciar sesión' })).not.toBeInTheDocument()
  })

  it('updates the account controls immediately after identity changes', async () => {
    const browserUser = userEvent.setup()
    render(
      <QueryClientProvider client={createQueryClient()}>
        <AuthProvider initialUser={null}>
          <MemoryRouter>
            <IdentityHarness />
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>,
    )

    await browserUser.click(screen.getByRole('button', { name: 'Simular acceso' }))

    expect(await screen.findByLabelText(/Abrir.*cuenta/)).toHaveTextContent('Sean')
    expect(screen.queryByRole('link', { name: 'Iniciar sesión' })).not.toBeInTheDocument()
  })
})
