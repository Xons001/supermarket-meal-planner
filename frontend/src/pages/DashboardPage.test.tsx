import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getDashboard } from '../api/dashboard'
import { createQueryClient } from '../app/queryClient'
import { DashboardPage } from './DashboardPage'

vi.mock('../api/dashboard', () => ({ getDashboard: vi.fn() }))
vi.mock('../components/SiteHeader', () => ({ SiteHeader: () => <header>Cabecera</header> }))

describe('DashboardPage', () => {
  beforeEach(() => vi.mocked(getDashboard).mockReset())

  it('renders persisted metrics and prioritised shopping list data', async () => {
    vi.mocked(getDashboard).mockResolvedValue({
      metrics: {
        activePlans: 2,
        favoritePlans: 1,
        shoppingLists: 3,
        currentShoppingLists: 1,
        outdatedShoppingLists: 1,
        averagePurchaseCost: 42,
        averageWasteCost: 3,
      },
      latestPlan: {
        id: 'plan-1',
        name: 'Semana equilibrada',
        startDate: '2026-08-03',
        strategy: 'PURCHASE_AWARE_SCORING',
        overallScore: 88,
        estimatedPurchaseCost: 42,
        estimatedWasteCost: 3,
        favorite: true,
        updatedAt: '2026-07-29T10:00:00Z',
      },
      selectedShoppingList: {
        id: 'list-1',
        mealPlanId: 'plan-1',
        mealPlanName: 'Semana equilibrada',
        totalPurchaseCost: 42,
        totalWasteCost: 3,
        freshness: 'CURRENT',
        generatedAt: '2026-07-29T10:00:00Z',
      },
      recentActivity: [],
    })
    renderPage()
    expect(await screen.findAllByRole('heading', { name: 'Semana equilibrada' })).toHaveLength(2)
    expect(screen.getByText('Actual')).toBeInTheDocument()
    expect(screen.getAllByText(/42,00/).length).toBeGreaterThanOrEqual(2)
  })

  it('renders the empty dashboard without fabricated values', async () => {
    vi.mocked(getDashboard).mockResolvedValue({
      metrics: {
        activePlans: 0,
        favoritePlans: 0,
        shoppingLists: 0,
        currentShoppingLists: 0,
        outdatedShoppingLists: 0,
        averagePurchaseCost: null,
        averageWasteCost: null,
      },
      latestPlan: null,
      selectedShoppingList: null,
      recentActivity: [],
    })
    renderPage()
    expect(await screen.findByText('Aún no tienes planes')).toBeInTheDocument()
    expect(screen.getByText('Sin lista seleccionada')).toBeInTheDocument()
  })
})

function renderPage() {
  render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}
