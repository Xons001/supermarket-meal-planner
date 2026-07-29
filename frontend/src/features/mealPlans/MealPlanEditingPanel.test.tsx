import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../../app/queryClient'
import { generatedMealPlanFixture } from '../../test/mealPlanFixtures'
import type { EditMetrics, GeneratedMealPlan } from '../../types/mealPlan'
import { MealPlanEditingPanel } from './MealPlanEditingPanel'

const planId = '10000000-0000-4000-8000-000000000001'
const dayId = '20000000-0000-4000-8000-000000000001'
const mealId = '30000000-0000-4000-8000-000000000001'

function editablePlan(): GeneratedMealPlan {
  return {
    ...generatedMealPlanFixture,
    persisted: true,
    mealPlanId: planId,
    status: 'GENERATED',
    editVersion: 0,
    contentVersion: 0,
    shoppingListStatus: 'CURRENT',
    canUndo: false,
    days: generatedMealPlanFixture.days.map((day) => ({
      ...day,
      dayId,
      meals: day.meals.map((meal) => ({
        ...meal,
        plannedMealId: mealId,
        locked: false,
        selectionSource: 'GENERATED',
      })),
    })),
  }
}

const metrics: EditMetrics = {
  calories: 500,
  protein: 35,
  consumedCost: 2,
  purchaseCost: 5,
  wasteCost: 1,
  wastePercentage: 20,
  packages: 3,
  uniqueProducts: 2,
  varietyScore: 80,
  repetitionScore: 90,
  overallScore: 82,
  budgetDifference: 10,
  budgetExceeded: false,
}

describe('MealPlanEditingPanel', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('loads ranked alternatives and requires an before/after preview', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn((input: RequestInfo | URL) => {
        const url = String(input)
        if (url.includes('/changes')) {
          return Promise.resolve(
            Response.json({
              content: [],
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
              first: true,
              last: true,
            }),
          )
        }
        if (url.includes('/alternatives')) {
          return Promise.resolve(
            Response.json([
              {
                rank: 1,
                mealTemplateId: '40000000-0000-4000-8000-000000000002',
                name: 'Alternativa equilibrada',
                mainIngredients: ['Arroz'],
                calories: 510,
                protein: 37,
                consumedCost: 2.1,
                marginalPurchaseCost: 0,
                purchaseCostDelta: -1,
                wasteCostDelta: -0.5,
                packageDelta: 0,
                uniqueProductDelta: 0,
                varietyDelta: 5,
                repetitionDelta: 0,
                estimatedScore: 86,
                reasons: ['Aprovecha un envase comprado'],
                warnings: [],
                seed: 42,
              },
            ]),
          )
        }
        return Promise.resolve(
          Response.json({
            operation: 'MEAL_REPLACED',
            planId,
            targetId: mealId,
            editVersion: 0,
            seed: 42,
            beforeMeals: editablePlan().days[0]!.meals,
            afterMeals: editablePlan().days[0]!.meals,
            before: metrics,
            after: { ...metrics, purchaseCost: 4, wasteCost: 0.5, overallScore: 86 },
            delta: {
              ...metrics,
              calories: 0,
              protein: 0,
              consumedCost: 0,
              purchaseCost: -1,
              wasteCost: -0.5,
              wastePercentage: -10,
              packages: 0,
              uniqueProducts: 0,
              varietyScore: 5,
              repetitionScore: 0,
              overallScore: 4,
              budgetDifference: 1,
              budgetExceeded: null,
            },
            reasons: ['Aprovecha un envase comprado'],
            warnings: [],
            previewToken: 'signed.preview',
            expiresAt: '2026-07-29T10:15:00Z',
            durationMilliseconds: 12,
          }),
        )
      }),
    )

    render(
      <QueryClientProvider client={createQueryClient()}>
        <MealPlanEditingPanel plan={editablePlan()} />
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Cambiar' }))
    expect(await screen.findByText('Alternativa equilibrada')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Ver antes y después' }))

    expect(await screen.findByRole('dialog', { name: 'Confirma el cambio' })).toBeInTheDocument()
    expect(screen.getByText('Antes')).toBeInTheDocument()
    expect(screen.getByText('Después')).toBeInTheDocument()
    expect(screen.getAllByText(/mejora/).length).toBeGreaterThan(0)
  })

  it('disables changing and regeneration for a locked meal', () => {
    const plan = editablePlan()
    plan.days[0]!.meals[0]!.locked = true
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MealPlanEditingPanel plan={plan} />
      </QueryClientProvider>,
    )
    expect(screen.getByRole('button', { name: 'Cambiar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Regenerar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: /Desbloquear/ })).toBeEnabled()
  })
})
