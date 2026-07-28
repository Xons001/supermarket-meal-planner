import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../../app/queryClient'
import { shoppingListFixture } from '../../test/shoppingListFixtures'
import { ShoppingListPanel } from './ShoppingListPanel'

describe('ShoppingListPanel', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('shows the empty state and generates a shopping list from the plan', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn((_input: RequestInfo | URL, init?: RequestInit) =>
      init?.method === 'POST'
        ? Promise.resolve(Response.json(shoppingListFixture, { status: 201 }))
        : Promise.resolve(Response.json({ detail: 'No existe' }, { status: 404 })),
    )
    vi.stubGlobal('fetch', fetchMock)
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MemoryRouter>
          <ShoppingListPanel mealPlanId={shoppingListFixture.mealPlanId} />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await user.click(await screen.findByRole('button', { name: 'Generar lista de compra' }))
    expect(await screen.findByText('Coste real de compra')).toBeInTheDocument()
    expect(screen.getByText('12,00 €')).toBeInTheDocument()
    expect(screen.getByText('Presupuesto')).toBeInTheDocument()
    expect(screen.getByText(/Presupuesto real superado/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver lista de compra' })).toHaveAttribute(
      'href',
      `/shopping-lists/${shoppingListFixture.id}`,
    )
  })

  it('requires confirmation before regeneration and archiving', async () => {
    const user = userEvent.setup()
    const confirm = vi.fn(() => false)
    vi.stubGlobal('confirm', confirm)
    const fetchMock = vi.fn(() => Promise.resolve(Response.json(shoppingListFixture)))
    vi.stubGlobal('fetch', fetchMock)
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MemoryRouter>
          <ShoppingListPanel mealPlanId={shoppingListFixture.mealPlanId} />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await user.click(await screen.findByRole('button', { name: 'Regenerar' }))
    await user.click(screen.getByRole('button', { name: 'Archivar lista' }))
    expect(confirm).toHaveBeenCalledTimes(2)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})
