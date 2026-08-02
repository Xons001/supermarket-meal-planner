import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '../api/nutritionAdmin'
import { createQueryClient } from '../app/queryClient'
import { NutritionAdminPage } from './NutritionAdminPage'

vi.mock('../api/nutritionAdmin')
vi.mock('../components/SiteHeader', () => ({ SiteHeader: () => <header>Cabecera</header> }))

const page = { page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true }

describe('NutritionAdminPage', () => {
  beforeEach(() => {
    vi.mocked(api.getNutritionOverview).mockResolvedValue({
      enabled: true,
      provider: 'LOCAL_JSON',
      cron: '0 4 * * 1',
      productsWithoutNutrition: 2,
      partialProducts: 1,
      verifiedProducts: 8,
      pendingCandidates: 1,
      latestRun: null,
    })
    vi.mocked(api.getNutritionRuns).mockResolvedValue({ ...page, content: [] })
    vi.mocked(api.getNutritionCandidates).mockResolvedValue({
      ...page,
      totalElements: 1,
      totalPages: 1,
      content: [
        {
          id: 'candidate-1',
          productId: 'product-1',
          productName: 'Yogur natural',
          currentSource: 'DEMO',
          provider: 'LOCAL_JSON',
          externalReference: 'demo-yogurt',
          externalName: 'Yogur natural',
          brand: 'Marca neutra',
          matchMethod: 'NAME_BRAND',
          confidenceScore: 88,
          scoreBreakdown: {},
          status: 'PENDING',
          rejectionReason: null,
          externalNutrition: { protein: 4 },
          currentNutrition: null,
          expiresAt: '2026-09-01T10:00:00Z',
          reviewedAt: null,
          version: 0,
        },
      ],
    })
    vi.mocked(api.triggerNutritionEnrichment).mockResolvedValue({ runId: 'run-1' })
    vi.mocked(api.acceptNutritionCandidate).mockResolvedValue({
      basis: 'PER_100_GRAMS',
      calories: 60,
      protein: 4,
      carbohydrates: 5,
      fat: 3,
      fiber: null,
      sugars: 5,
      salt: 0.1,
      saturatedFat: 2,
      dataSource: 'LOCAL_JSON',
      verificationStatus: 'VERIFIED',
      confidenceScore: 88,
      completeness: 'PARTIAL',
      sourceReference: 'demo-yogurt',
      updatedAt: '2026-08-02T10:00:00Z',
    })
  })

  it('shows quality metrics and an explainable candidate', async () => {
    renderPage()
    expect(await screen.findByRole('heading', { name: 'Calidad nutricional' })).toBeInTheDocument()
    expect(await screen.findAllByText('Yogur natural')).toHaveLength(2)
    expect(screen.getByText('88%')).toBeInTheDocument()
    expect(screen.getByText(/NAME_BRAND/)).toBeInTheDocument()
  })

  it('triggers a local enrichment from the admin action', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Enriquecer nutrición' }))
    expect(api.triggerNutritionEnrichment).toHaveBeenCalledWith('LOCAL_JSON')
  })
})

function renderPage() {
  render(
    <QueryClientProvider client={createQueryClient()}>
      <NutritionAdminPage />
    </QueryClientProvider>,
  )
}
