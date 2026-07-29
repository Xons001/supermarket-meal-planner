import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import {
  mealTemplateFixture,
  mealTemplatePage,
  supermarketsFixture,
} from '../test/mealTemplateFixtures'
import { MealTemplatesPage } from './MealTemplatesPage'
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

const tags = [
  { id: 'tag-1', code: 'HIGH_PROTEIN', name: 'Alto en proteína' },
  { id: 'tag-2', code: 'VEGAN', name: 'Vegano' },
]
const allergens = [
  { id: 'allergen-1', code: 'MILK', name: 'Leche' },
  { id: 'allergen-2', code: 'GLUTEN', name: 'Gluten' },
]

function mockApi({
  page = mealTemplatePage(),
  fail = false,
}: {
  page?: ReturnType<typeof mealTemplatePage>
  fail?: boolean
} = {}) {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const url = String(input)
    if (url.includes('/actuator/health')) return Promise.resolve(Response.json({ status: 'UP' }))
    if (url.includes('/api/v1/supermarkets'))
      return Promise.resolve(Response.json(supermarketsFixture))
    if (url.includes('/api/v1/dietary-tags')) return Promise.resolve(Response.json(tags))
    if (url.includes('/api/v1/allergens')) return Promise.resolve(Response.json(allergens))
    if (url.includes('/api/v1/meal-templates')) {
      return Promise.resolve(
        fail ? Response.json({ detail: 'Fallo controlado' }, { status: 500 }) : Response.json(page),
      )
    }
    return Promise.resolve(Response.json({}, { status: 404 }))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function LocationDisplay() {
  const location = useLocation()
  return <output data-testid="location">{`${location.pathname}${location.search}`}</output>
}

function renderPage(initialEntry = '/meal-templates') {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <AuthProvider initialUser={admin}>
        <MemoryRouter initialEntries={[initialEntry]}>
          <Routes>
            <Route
              path="/meal-templates"
              element={
                <>
                  <MealTemplatesPage />
                  <LocationDisplay />
                </>
              }
            />
            <Route path="/meal-templates/:id" element={<h1>Detalle abierto</h1>} />
            <Route path="/meal-templates/new" element={<h1>Formulario abierto</h1>} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('MealTemplatesPage', () => {
  beforeEach(() => mockApi())
  afterEach(() => vi.unstubAllGlobals())

  it('renders filters, calculated cards and create access', async () => {
    renderPage()
    expect(screen.getByRole('heading', { name: /comidas calculadas/i })).toBeInTheDocument()
    expect(await screen.findByText(mealTemplateFixture.name)).toBeInTheDocument()
    expect(screen.getByText(/cálculo parcial/i)).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Vegano' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Leche' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /crear plantilla/i })).toBeInTheDocument()
  })

  it('restores filters from URL and sends them to the API', async () => {
    const fetchMock = mockApi()
    renderPage('/meal-templates?query=pollo&mealType=LUNCH&tags=HIGH_PROTEIN&exclude=MILK')
    expect(await screen.findByDisplayValue('pollo')).toBeInTheDocument()
    expect(await screen.findByRole('checkbox', { name: 'Alto en proteína' })).toBeChecked()
    expect(await screen.findByRole('checkbox', { name: 'Leche' })).toBeChecked()
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url]) => {
          const value = String(url)
          return (
            value.includes('query=pollo') &&
            value.includes('mealType=LUNCH') &&
            value.includes('dietaryTags=HIGH_PROTEIN') &&
            value.includes('excludedAllergens=MILK')
          )
        }),
      ).toBe(true),
    )
  })

  it('debounces search and can clear all filters', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.type(await screen.findByRole('searchbox'), 'lentejas')
    expect(screen.getByTestId('location')).not.toHaveTextContent('query=lentejas')
    await waitFor(
      () => expect(screen.getByTestId('location')).toHaveTextContent('query=lentejas'),
      { timeout: 1200 },
    )
    await user.click(screen.getByRole('button', { name: /limpiar filtros/i }))
    expect(screen.getByRole('searchbox')).toHaveValue('')
    expect(screen.getByTestId('location')).toHaveTextContent(
      '/meal-templates?supermarket=MERCADONA',
    )
  })

  it('shows empty and error states', async () => {
    vi.unstubAllGlobals()
    mockApi({ page: mealTemplatePage([]) })
    const view = renderPage()
    expect(await screen.findByText(/no hay plantillas/i)).toBeInTheDocument()
    view.unmount()

    vi.unstubAllGlobals()
    mockApi({ fail: true })
    renderPage()
    expect(await screen.findByRole('alert', {}, { timeout: 2500 })).toHaveTextContent(
      /no se han podido cargar/i,
    )
  })

  it('navigates to detail and creation routes', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(await screen.findByRole('link', { name: /ver plantilla arroz/i }))
    expect(await screen.findByRole('heading', { name: 'Detalle abierto' })).toBeInTheDocument()
  })
})
