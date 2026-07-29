import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { mealTemplateFixture, partialMealTemplateFixture } from '../test/mealTemplateFixtures'
import type { MealTemplate } from '../types/api'
import { MealTemplateDetailPage } from './MealTemplateDetailPage'
import { AuthProvider } from '../auth/AuthProvider'
import type { AuthUser } from '../types/auth'

const admin: AuthUser = {
  id: '00000000-0000-4000-8000-000000000001',
  email: 'admin@example.test',
  displayName: 'Admin',
  status: 'ACTIVE',
  role: 'ADMIN',
  createdAt: '2026-01-01T00:00:00Z',
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
  },
}

function mockApi(template: MealTemplate = mealTemplateFixture) {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (url.includes('/actuator/health')) return Promise.resolve(Response.json({ status: 'UP' }))
    if (init?.method === 'PATCH') {
      return Promise.resolve(Response.json({ ...template, active: !template.active }))
    }
    if (init?.method === 'DELETE') {
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    if (url.includes(`/api/v1/meal-templates/${template.id}`)) {
      return Promise.resolve(Response.json(template))
    }
    return Promise.resolve(Response.json({ detail: 'No encontrado' }, { status: 404 }))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function renderPage(template: MealTemplate = mealTemplateFixture) {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <AuthProvider initialUser={admin}>
        <MemoryRouter initialEntries={[`/meal-templates/${template.id}`]}>
          <Routes>
            <Route path="/meal-templates/:id" element={<MealTemplateDetailPage />} />
            <Route path="/meal-templates" element={<h1>Listado abierto</h1>} />
            <Route path="/meal-templates/:id/edit" element={<h1>Editor abierto</h1>} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('MealTemplateDetailPage', () => {
  beforeEach(() => mockApi())
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('renders instructions, ingredients, total and per-serving values', async () => {
    renderPage()
    expect(
      await screen.findByRole('heading', { name: mealTemplateFixture.name }),
    ).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Instrucciones' })).toBeInTheDocument()
    expect(screen.getByText('Cocer el arroz.')).toBeInTheDocument()
    expect(screen.getByText('Arroz largo')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Valores totales' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Valores por ración' })).toBeInTheDocument()
    expect(screen.getByText(/coste mostrado es el valor proporcional/i)).toBeInTheDocument()
  })

  it('renders partial calculation and missing nutrition warnings', async () => {
    vi.unstubAllGlobals()
    mockApi(partialMealTemplateFixture)
    renderPage(partialMealTemplateFixture)
    expect(
      await screen.findByRole('heading', { name: partialMealTemplateFixture.name }),
    ).toBeInTheDocument()
    expect(screen.getAllByText(/no hay información nutricional/i).length).toBeGreaterThan(0)
    expect(screen.getAllByText('No disponible').length).toBeGreaterThan(0)
    expect(screen.getByText('Desactivada')).toBeInTheDocument()
  })

  it('changes status and opens the editor', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage()
    await screen.findByRole('heading', { name: mealTemplateFixture.name })
    await user.click(screen.getByRole('button', { name: 'Desactivar' }))
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PATCH')).toBe(true)
    await user.click(screen.getByRole('link', { name: 'Editar' }))
    expect(await screen.findByRole('heading', { name: 'Editor abierto' })).toBeInTheDocument()
  })

  it('requires confirmation before archiving', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()
    await screen.findByRole('heading', { name: mealTemplateFixture.name })
    await user.click(screen.getByRole('button', { name: 'Archivar' }))
    expect(window.confirm).toHaveBeenCalled()
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(true)
    expect(await screen.findByRole('heading', { name: 'Listado abierto' })).toBeInTheDocument()
  })
})
