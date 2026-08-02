import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import type { Product } from '../types/api'
import { ProductsPage } from './ProductsPage'

const product: Product = {
  id: '10000000-0000-0000-0000-000000000001',
  supermarketCode: 'MERCADONA',
  supermarketName: 'Mercadona',
  categoryId: '20000000-0000-0000-0000-000000000001',
  categoryName: 'Carnes y huevos',
  externalId: 'demo-chicken',
  barcode: 'DEMO-1',
  name: 'Pechuga de pollo',
  brand: 'Marca neutra',
  description: 'Producto ficticio',
  imageUrl: null,
  currentPrice: 4.75,
  unitPrice: 9.5,
  packageQuantity: 500,
  packageUnit: 'G',
  measurementType: 'WEIGHT',
  costDataComplete: true,
  available: true,
  source: 'DEMO_JSON',
  lastSyncedAt: '2026-07-28T00:00:00Z',
  demonstrationData: true,
  dietaryTags: [
    {
      id: '30000000-0000-0000-0000-000000000001',
      code: 'HIGH_PROTEIN',
      name: 'Alto en proteína',
    },
  ],
  allergens: [],
  nutrition: {
    caloriesPer100g: 110,
    proteinPer100g: 23.1,
    carbohydratesPer100g: 0,
    fatPer100g: 1.9,
    fiberPer100g: 0,
    sugarPer100g: 0,
    saltPer100g: 0.13,
    saturatedFatPer100g: null,
    nutritionBasis: 'PER_100_GRAMS',
    completeness: 'COMPLETE',
    sourceReference: null,
    sourceUpdatedAt: null,
    perUnit: null,
    dataSource: 'CONTROLLED_DEMO_DATA',
    verificationStatus: 'DEMO',
    confidenceScore: 1,
    updatedAt: '2026-07-28T00:00:00Z',
  },
}

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

const categories = [
  {
    id: product.categoryId,
    externalId: 'demo-cat-meat-eggs',
    name: product.categoryName,
    parentCategoryId: null,
    supermarketCode: 'MERCADONA',
  },
]

const dietaryTags = [
  { id: 'tag-1', code: 'HIGH_PROTEIN', name: 'Alto en proteína' },
  { id: 'tag-2', code: 'VEGAN', name: 'Vegano' },
]

const allergens = [
  { id: 'allergen-1', code: 'MILK', name: 'Leche' },
  { id: 'allergen-2', code: 'GLUTEN', name: 'Gluten' },
]

function pageResponse(content: Product[] = [product]) {
  return {
    content,
    page: 0,
    size: 12,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    first: true,
    last: true,
  }
}

function mockApi({
  products = pageResponse(),
  failProducts = false,
}: {
  products?: ReturnType<typeof pageResponse>
  failProducts?: boolean
} = {}) {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const url = String(input)
    if (url.includes('/actuator/health')) {
      return Promise.resolve(Response.json({ status: 'UP' }))
    }
    if (url.includes('/api/v1/supermarkets')) {
      return Promise.resolve(Response.json(supermarkets))
    }
    if (url.includes('/api/v1/categories')) {
      return Promise.resolve(Response.json(categories))
    }
    if (url.includes('/api/v1/dietary-tags')) {
      return Promise.resolve(Response.json(dietaryTags))
    }
    if (url.includes('/api/v1/allergens')) {
      return Promise.resolve(Response.json(allergens))
    }
    if (url.includes('/api/v1/products')) {
      return Promise.resolve(
        failProducts
          ? Response.json({ detail: 'Fallo controlado' }, { status: 500 })
          : Response.json(products),
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

function renderPage(initialEntry = '/products') {
  const queryClient = createQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route
            path="/products"
            element={
              <>
                <ProductsPage />
                <LocationDisplay />
              </>
            }
          />
          <Route path="/products/:id" element={<h1>Detalle abierto</h1>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ProductsPage', () => {
  beforeEach(() => {
    mockApi()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the catalog and loads every filter group', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: /encuentra productos/i })).toBeInTheDocument()
    expect(await screen.findByText('Pechuga de pollo')).toBeInTheDocument()
    expect(await screen.findByRole('option', { name: 'Carnes y huevos' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Alto en proteína' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Leche' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: /carrefour.*próximamente/i })).toBeDisabled()
  })

  it('reads search, tags and allergen exclusions from the URL', async () => {
    const fetchMock = mockApi()
    renderPage('/products?supermarket=MERCADONA&query=pollo&tags=HIGH_PROTEIN&exclude=MILK')

    expect(await screen.findByDisplayValue('pollo')).toBeInTheDocument()
    expect(await screen.findByRole('checkbox', { name: 'Alto en proteína' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Leche' })).toBeChecked()
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url]) => {
          const value = String(url)
          return (
            value.includes('query=pollo') &&
            value.includes('dietaryTags=HIGH_PROTEIN') &&
            value.includes('excludedAllergens=MILK')
          )
        }),
      ).toBe(true),
    )
  })

  it('debounces text search before updating URL and requesting products', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage()

    const search = await screen.findByRole('searchbox', { name: /buscar por nombre/i })
    await user.type(search, 'tofu')

    expect(screen.getByTestId('location')).not.toHaveTextContent('query=tofu')
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('query=tofu'), {
      timeout: 1200,
    })
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('query=tofu'))).toBe(true)
  })

  it('updates dietary and allergen filters, then clears all filters', async () => {
    const user = userEvent.setup()
    renderPage('/products?query=pollo')

    const tag = await screen.findByRole('checkbox', { name: 'Vegano' })
    const allergen = screen.getByRole('checkbox', { name: 'Gluten' })
    await user.click(tag)
    await user.click(allergen)

    expect(screen.getByTestId('location')).toHaveTextContent('tags=VEGAN')
    expect(screen.getByTestId('location')).toHaveTextContent('exclude=GLUTEN')

    await user.click(screen.getByRole('button', { name: /limpiar filtros/i }))
    expect(screen.getByRole('searchbox')).toHaveValue('')
    expect(tag).not.toBeChecked()
    expect(allergen).not.toBeChecked()
    expect(screen.getByTestId('location')).toHaveTextContent('/products?supermarket=MERCADONA')
  })

  it('shows empty and API error states', async () => {
    vi.unstubAllGlobals()
    mockApi({ products: pageResponse([]) })
    const view = renderPage()
    expect(await screen.findByText(/no hay productos/i)).toBeInTheDocument()

    view.unmount()
    vi.unstubAllGlobals()
    mockApi({ failProducts: true })
    renderPage()
    expect(await screen.findByRole('alert', {}, { timeout: 2500 })).toHaveTextContent(
      /no se ha podido cargar/i,
    )
  })

  it('navigates from a product card to its detail route', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('link', { name: /ver detalle de pechuga/i }))
    expect(await screen.findByRole('heading', { name: 'Detalle abierto' })).toBeInTheDocument()
  })
})
