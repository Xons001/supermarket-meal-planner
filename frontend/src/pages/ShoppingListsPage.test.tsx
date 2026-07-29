import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { shoppingListSummaryFixture } from '../test/shoppingListFixtures'
import { ShoppingListsPage } from './ShoppingListsPage'

function renderPage(empty = false) {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const url = String(input)
    if (url.includes('/actuator/health')) return Promise.resolve(Response.json({ status: 'UP' }))
    if (url.includes('/supermarkets')) return Promise.resolve(Response.json([]))
    return Promise.resolve(
      Response.json({
        content: empty ? [] : [shoppingListSummaryFixture],
        page: 0,
        size: 9,
        totalElements: empty ? 0 : 1,
        totalPages: empty ? 0 : 1,
        first: true,
        last: true,
      }),
    )
  })
  vi.stubGlobal('fetch', fetchMock)
  render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter initialEntries={['/shopping-lists']}>
        <ShoppingListsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return fetchMock
}

describe('ShoppingListsPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('renders summaries, links and sends filters', async () => {
    const user = userEvent.setup()
    const fetchMock = renderPage()
    expect(await screen.findByText('Plan semanal demo')).toBeInTheDocument()
    expect(screen.getByText('Cálculo parcial')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver plan' })).toHaveAttribute(
      'href',
      `/meal-plans/${shoppingListSummaryFixture.mealPlanId}`,
    )
    await user.selectOptions(screen.getByLabelText('Estado'), 'GENERATED')
    await waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('status=GENERATED'))).toBe(
        true,
      ),
    )
  })

  it('shows an empty state', async () => {
    renderPage(true)
    expect(await screen.findByText(/no hay listas de compra/i)).toBeInTheDocument()
  })
})
