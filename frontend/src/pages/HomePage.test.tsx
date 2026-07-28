import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import { HomePage } from './HomePage'

const supermarkets = [
  {
    code: 'MERCADONA',
    name: 'Mercadona',
    enabled: true,
    catalogSource: 'DEMO_JSON',
    countryCode: 'ES',
    currencyCode: 'EUR',
  },
  {
    code: 'CARREFOUR',
    name: 'Carrefour',
    enabled: false,
    catalogSource: 'PLANNED',
    countryCode: 'ES',
    currencyCode: 'EUR',
  },
]

const products = {
  content: [
    {
      id: '10000000-0000-0000-0000-000000000001',
      supermarketCode: 'MERCADONA',
      supermarketName: 'Mercadona',
      categoryId: '20000000-0000-0000-0000-000000000001',
      categoryName: 'Proteínas',
      externalId: 'demo-chicken',
      barcode: 'DEMO-1',
      name: 'Pechuga de pollo',
      brand: 'Marca genérica',
      description: 'Producto ficticio',
      currentPrice: 4.75,
      unitPrice: 9.5,
      packageQuantity: 500,
      packageUnit: 'G',
      available: true,
      source: 'DEMO_JSON',
      lastSyncedAt: '2026-07-28T00:00:00Z',
      demonstrationData: true,
      nutrition: {
        caloriesPer100g: 110,
        proteinPer100g: 23.1,
        carbohydratesPer100g: 0,
        fatPer100g: 1.9,
        fiberPer100g: 0,
        sugarPer100g: 0,
        saltPer100g: 0.13,
        dataSource: 'CONTROLLED_DEMO_DATA',
        verificationStatus: 'DEMO',
        confidenceScore: 1,
        updatedAt: '2026-07-28T00:00:00Z',
      },
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
}

function renderPage() {
  const queryClient = createQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn((input: RequestInfo | URL) => {
        const url = String(input)
        if (url.includes('/actuator/health')) {
          return Promise.resolve(Response.json({ status: 'UP' }))
        }
        if (url.includes('/api/v1/supermarkets')) {
          return Promise.resolve(Response.json(supermarkets))
        }
        if (url.includes('/api/v1/products')) {
          return Promise.resolve(Response.json(products))
        }
        return Promise.resolve(Response.json({}, { status: 404 }))
      }),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the connected landing page and demo catalog', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: /tu semana de comidas/i })).toBeInTheDocument()
    expect(await screen.findByText('Backend conectado')).toBeInTheDocument()
    expect(await screen.findByText('Pechuga de pollo')).toBeInTheDocument()
    expect(screen.getAllByText('Datos de demostración').length).toBeGreaterThan(0)
  })

  it('enables the available supermarket and disables future providers', async () => {
    const user = userEvent.setup()
    renderPage()

    const availableOption = await screen.findByRole('radio', { name: /mercadona/i })
    const futureOption = screen.getByRole('radio', { name: /carrefour/i })

    expect(availableOption).toBeEnabled()
    expect(futureOption).toBeDisabled()

    await user.click(availableOption)
    await user.click(screen.getByRole('button', { name: /ver catálogo/i }))

    await waitFor(() => expect(availableOption).toBeChecked())
  })
})
