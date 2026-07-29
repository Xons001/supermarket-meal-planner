import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { mealPlanSummaryFixture } from '../test/mealPlanFixtures'
import { MealPlansPage } from './MealPlansPage'

function renderPage(fail = false, empty = false) {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const url = String(input)
    if (url.includes('/actuator/health')) return Promise.resolve(Response.json({ status: 'UP' }))
    if (url.includes('/api/v1/meal-plans')) {
      return Promise.resolve(
        fail
          ? Response.json({ detail: 'Error controlado' }, { status: 500 })
          : Response.json({
              content: empty ? [] : [mealPlanSummaryFixture],
              page: 0,
              size: 9,
              totalElements: empty ? 0 : 1,
              totalPages: empty ? 0 : 1,
              first: true,
              last: true,
            }),
      )
    }
    return Promise.resolve(Response.json({}, { status: 404 }))
  })
  vi.stubGlobal('fetch', fetchMock)
  render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter initialEntries={['/meal-plans']}>
        <MealPlansPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return fetchMock
}

describe('MealPlansPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('renders saved plans and sends URL filters', async () => {
    const user = userEvent.setup()
    const fetchMock = renderPage()
    expect(await screen.findByText('Plan semanal demo')).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Estado'), 'GENERATED')
    await waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('status=GENERATED'))).toBe(
        true,
      ),
    )
    expect(screen.getByRole('link', { name: /crear plan/i })).toHaveAttribute(
      'href',
      '/meal-plans/new',
    )
  })

  it('shows empty and error states', async () => {
    const emptyView = renderPage(false, true)
    expect(await screen.findByText(/todavía no hay planes/i)).toBeInTheDocument()
    emptyView.mockClear()
    vi.unstubAllGlobals()

    renderPage(true)
    expect(await screen.findByRole('alert', {}, { timeout: 2500 })).toHaveTextContent(
      /no se han podido cargar/i,
    )
  })
})
