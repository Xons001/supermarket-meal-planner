import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { shoppingListFixture } from '../test/shoppingListFixtures'
import { ShoppingListDetailPage } from './ShoppingListDetailPage'

describe('ShoppingListDetailPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('shows grouped products, partial values, availability, export and printing', async () => {
    const user = userEvent.setup()
    const print = vi.fn()
    vi.stubGlobal('print', print)
    vi.stubGlobal(
      'fetch',
      vi.fn((input: RequestInfo | URL) =>
        String(input).includes('/actuator')
          ? Promise.resolve(Response.json({ status: 'UP' }))
          : Promise.resolve(Response.json(shoppingListFixture)),
      ),
    )
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MemoryRouter initialEntries={[`/shopping-lists/${shoppingListFixture.id}`]}>
          <Routes>
            <Route path="/shopping-lists/:id" element={<ShoppingListDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('heading', { name: 'Plan semanal demo' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Carnes' })).toBeInTheDocument()
    expect(screen.getByText('Pechuga de pollo')).toBeInTheDocument()
    expect(screen.getAllByText('No calculable').length).toBeGreaterThan(1)
    expect(screen.getByText('No disponible')).toBeInTheDocument()
    expect(screen.getByText(/presupuesto real superado/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Exportar CSV' })).toHaveAttribute(
      'href',
      `/api/v1/shopping-lists/${shoppingListFixture.id}/export?format=csv`,
    )
    await user.click(screen.getByRole('button', { name: 'Imprimir' }))
    expect(print).toHaveBeenCalledOnce()
  })

  it('shows the not found state', async () => {
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
        <MemoryRouter initialEntries={['/shopping-lists/missing']}>
          <Routes>
            <Route path="/shopping-lists/:id" element={<ShoppingListDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )
    expect(await screen.findByRole('alert', {}, { timeout: 2500 })).toHaveTextContent(
      /no se ha encontrado/i,
    )
  })
})
