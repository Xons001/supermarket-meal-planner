import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { generatedMealPlanFixture, mealPlanSummaryFixture } from '../test/mealPlanFixtures'
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
    expect(screen.getByText('Arroz con pollo')).toBeInTheDocument()
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
})
