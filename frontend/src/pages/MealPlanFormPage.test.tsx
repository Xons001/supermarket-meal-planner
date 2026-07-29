import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { mealTemplatePage, productFixture, supermarketsFixture } from '../test/mealTemplateFixtures'
import { generatedMealPlanFixture, mealPlanSummaryFixture } from '../test/mealPlanFixtures'
import { MealPlanFormPage } from './MealPlanFormPage'

const tags = [{ id: 'tag-1', code: 'HIGH_PROTEIN', name: 'Alto en proteína' }]
const allergens = [{ id: 'allergen-1', code: 'MILK', name: 'Leche' }]

function mockApi(generateFailure = false) {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (url.includes('/actuator/health')) return Promise.resolve(Response.json({ status: 'UP' }))
    if (url.includes('/api/v1/supermarkets'))
      return Promise.resolve(Response.json(supermarketsFixture))
    if (url.includes('/api/v1/dietary-tags')) return Promise.resolve(Response.json(tags))
    if (url.includes('/api/v1/allergens')) return Promise.resolve(Response.json(allergens))
    if (url.includes('/api/v1/meal-templates'))
      return Promise.resolve(Response.json(mealTemplatePage()))
    if (url.includes('/api/v1/products'))
      return Promise.resolve(
        Response.json({
          content: [productFixture],
          page: 0,
          size: 48,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        }),
      )
    if (url.includes('/api/v1/meal-plans/generate')) {
      if (generateFailure) {
        return Promise.resolve(
          Response.json(
            {
              detail: 'No quedan plantillas viables. Reduce las restricciones.',
              status: 422,
            },
            { status: 422 },
          ),
        )
      }
      const request = JSON.parse(String(init?.body)) as { persist: boolean }
      return Promise.resolve(
        Response.json(
          request.persist
            ? {
                ...generatedMealPlanFixture,
                persisted: true,
                mealPlanId: mealPlanSummaryFixture.id,
                status: 'GENERATED',
              }
            : generatedMealPlanFixture,
        ),
      )
    }
    return Promise.resolve(Response.json({}, { status: 404 }))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function renderPage() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter initialEntries={['/meal-plans/new']}>
        <Routes>
          <Route path="/meal-plans/new" element={<MealPlanFormPage />} />
          <Route path="/meal-plans/:id" element={<h1>Plan guardado abierto</h1>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('MealPlanFormPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('renders all sections, previews and persists the exact seed and token', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage()

    expect(screen.getByLabelText('Modo de generación')).toHaveValue('PURCHASE_AWARE_SCORING')
    expect(screen.getByLabelText('Prioridad')).toHaveValue('BALANCED')
    expect(screen.getByRole('heading', { name: /construye tu plan/i })).toBeInTheDocument()
    expect(await screen.findByRole('checkbox', { name: 'Alto en proteína' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Leche' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /generar previsualización/i }))

    expect(await screen.findByText('62,5 / 100')).toBeInTheDocument()
    expect(screen.getByText('Arroz con pollo')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /guardar este plan/i }))
    expect(
      await screen.findByRole('heading', { name: 'Plan guardado abierto' }),
    ).toBeInTheDocument()

    const generateCalls = fetchMock.mock.calls.filter(([url]) =>
      String(url).includes('/api/v1/meal-plans/generate'),
    )
    expect(generateCalls).toHaveLength(2)
    const savedBody = JSON.parse(String(generateCalls[1]?.[1]?.body)) as Record<string, unknown>
    expect(savedBody).toMatchObject({
      persist: true,
      deterministicSeed: 123456,
      generationToken: 'a'.repeat(64),
      strategy: 'PURCHASE_AWARE_SCORING',
      optimizationPreset: 'BALANCED',
    })
  })

  it('hides purchase presets and sends no preset in classic mode', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage()

    await user.selectOptions(screen.getByLabelText('Modo de generación'), 'SCORING')
    expect(screen.queryByLabelText('Prioridad')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /generar previsual/i }))

    const generateCall = fetchMock.mock.calls.find(([url]) =>
      String(url).includes('/api/v1/meal-plans/generate'),
    )
    const body = JSON.parse(String(generateCall?.[1]?.body)) as Record<string, unknown>
    expect(body.strategy).toBe('SCORING')
    expect(body.optimizationPreset).toBeUndefined()
  })

  it('validates ranges before sending a request', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage()
    const days = screen.getByLabelText(/número de días/i)
    await user.clear(days)
    await user.type(days, '15')
    await user.click(screen.getByRole('button', { name: /generar previsualización/i }))
    expect(days).toBeInvalid()
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/meal-plans/generate'))).toBe(
      false,
    )
  })

  it('shows the API explanation when generation is impossible', async () => {
    const user = userEvent.setup()
    mockApi(true)
    renderPage()
    await user.click(screen.getByRole('button', { name: /generar previsualización/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/reduce las restricciones/i)
  })
})
