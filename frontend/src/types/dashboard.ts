import type { PageResponse } from './api'

export type ActivityOrigin = 'LIVE' | 'BACKFILLED'

export interface ActivityItem {
  id: string
  type: string
  summary: string
  occurredAt: string
  origin: ActivityOrigin
  resourceType: 'MEAL_PLAN' | 'SHOPPING_LIST'
  resourceId: string
  link: string
  deltas: Record<string, unknown>
}

export interface Dashboard {
  metrics: {
    activePlans: number
    favoritePlans: number
    shoppingLists: number
    currentShoppingLists: number
    outdatedShoppingLists: number
    averagePurchaseCost: number | null
    averageWasteCost: number | null
  }
  latestPlan: {
    id: string
    name: string
    startDate: string
    strategy: string
    overallScore: number
    estimatedPurchaseCost: number | null
    estimatedWasteCost: number | null
    favorite: boolean
    updatedAt: string
  } | null
  selectedShoppingList: {
    id: string
    mealPlanId: string
    mealPlanName: string
    totalPurchaseCost: number
    totalWasteCost: number
    freshness: 'CURRENT' | 'OUTDATED'
    generatedAt: string
  } | null
  recentActivity: ActivityItem[]
}

export type ActivityPage = PageResponse<ActivityItem>
