import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import {
  generatedMealPlanFixture,
  mealPlanSummaryFixture,
  purchaseAwareMealPlanFixture,
} from '../test/mealPlanFixtures'
import { MealPlanDetailPage } from './MealPlanDetailPage'

describe('MealPlanDetailPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('shows the snapshot, warnings, days and archives it', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'confirm',
      vi.fn(() => true),
    )
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.includes('/actuator/health')) return Promise.resolve(Response.json({ status: 'UP' }))
      if (url.includes('/shopping-list')) {
        return Promise.resolve(Response.json({ detail: 'No existe' }, { status: 404 }))
      }
      if (init?.method === 'DELETE') return Promise.resolve(new Response(null, { status: 204 }))
      return Promise.resolve(
        Response.json({
          ...generatedMealPlanFixture,
          persisted: true,
          mealPlanId: mealPlanSummaryFixture.id,
          status: 'GENERATED',
        }),
      )
    })
    vi.stubGlobal('fetch', fetchMock)
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MemoryRouter initialEntries={[`/meal-plans/${mealPlanSummaryFixture.id}`]}>
          <Routes>
            <Route path="/meal-plans/:id" element={<MealPlanDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('heading', { name: 'Plan semanal demo' })).toBeInTheDocument()
    expect(screen.getAllByText('Arroz con pollo').length).toBeGreaterThan(0)
    expect(screen.getByText(/presupuesto usa coste consumido/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Archivar' }))
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(true)
  })

  it('shows a not found state', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((input: RequestInfo | URL) =>
        String(input).includes('/actuator')
          ? Promise.resolve(Response.json({ status: 'UP' }))
          : Promise.resolve(Response.json({ detail: 'No existe' }, { status: 404 })),
      ),
    )
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MemoryRouter initialEntries={['/meal-plans/missing']}>
          <Routes>
            <Route path="/meal-plans/:id" element={<MealPlanDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )
    expect(await screen.findByRole('alert', {}, { timeout: 2500 })).toHaveTextContent(
      /no se ha encontrado/i,
    )
  })

  it('shows purchase metrics, budget margin, score breakdown and deterministic reasons', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((input: RequestInfo | URL) => {
        const url = String(input)
        if (url.includes('/actuator')) return Promise.resolve(Response.json({ status: 'UP' }))
        if (url.includes('/shopping-list')) {
          return Promise.resolve(Response.json({ detail: 'No existe' }, { status: 404 }))
        }
        return Promise.resolve(
          Response.json({
            ...purchaseAwareMealPlanFixture,
            persisted: true,
            mealPlanId: mealPlanSummaryFixture.id,
            status: 'GENERATED',
          }),
        )
      }),
    )
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MemoryRouter initialEntries={[`/meal-plans/${mealPlanSummaryFixture.id}`]}>
          <Routes>
            <Route path="/meal-plans/:id" element={<MealPlanDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    const purchase = await screen.findByRole('region', { name: 'Optimización de compra' })
    expect(within(purchase).getByText('53,35 €')).toBeInTheDocument()
    expect(within(purchase).getByText('15,82 €')).toBeInTheDocument()
    expect(within(purchase).getByText('29,7 %')).toBeInTheDocument()
    expect(within(purchase).getByText('16,65 € de margen')).toBeInTheDocument()
    expect(within(purchase).getByText(/reutiliza ingredientes/i)).toBeInTheDocument()
    expect(screen.getByText('Coste real de compra')).toBeInTheDocument()
    expect(screen.getAllByText('Reutilización útil')).toHaveLength(2)
  })
})
