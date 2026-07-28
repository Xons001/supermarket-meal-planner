import type { PageResponse } from './api'

export type ShoppingListStatus = 'GENERATED' | 'ARCHIVED'
export type MeasurementType = 'WEIGHT' | 'VOLUME' | 'UNIT'

export interface ShoppingListWarning {
  code: string
  message: string
  severity: 'INFO' | 'WARNING' | 'ERROR'
  itemId: string | null
}

export interface ShoppingListItem {
  id: string
  productId: string
  productName: string
  brand: string | null
  categoryId: string | null
  categoryName: string | null
  measurementType: MeasurementType | null
  requiredQuantity: number
  requiredUnit: string
  packageQuantity: number | null
  packageUnit: string | null
  packagePrice: number | null
  unitPrice: number | null
  packagesRequired: number | null
  purchasedQuantity: number | null
  leftoverQuantity: number | null
  consumedCost: number | null
  purchaseCost: number | null
  wasteCost: number | null
  leftoverPercentage: number | null
  available: boolean | null
  calculationComplete: boolean
  sortOrder: number
  warnings: string[]
}

export interface ShoppingListGroup {
  categoryId: string | null
  categoryName: string
  items: ShoppingListItem[]
  subtotalPurchaseCost: number
  subtotalConsumedCost: number
  subtotalWasteCost: number
  calculationComplete: boolean
}

export interface QuantitySummary {
  required: number
  purchased: number
  leftover: number
  unit: string
  calculationComplete: boolean
}

export interface ShoppingList {
  id: string
  mealPlanId: string
  mealPlanName: string
  supermarket: {
    id: string
    code: string
    name: string
    currencyCode: string
  }
  status: ShoppingListStatus
  generatedAt: string
  updatedAt: string
  groups: ShoppingListGroup[]
  itemCount: number
  totalPackages: number
  totalConsumedCost: number
  totalPurchaseCost: number
  totalWasteCost: number
  overallWastePercentage: number
  quantitySummary: Record<MeasurementType, QuantitySummary>
  weeklyBudget: number | null
  purchaseBudgetDifference: number | null
  purchaseBudgetExceeded: boolean
  purchaseBudgetDeviationPercentage: number | null
  budgetCalculationComplete: boolean
  calculationComplete: boolean
  warnings: ShoppingListWarning[]
  demoData: boolean
  generationDurationMilliseconds: number
}

export interface ShoppingListSummary {
  id: string
  mealPlanId: string
  mealPlanName: string
  supermarketCode: string
  supermarketName: string
  status: ShoppingListStatus
  generatedAt: string
  itemCount: number
  totalPackages: number
  totalConsumedCost: number
  totalPurchaseCost: number
  totalWasteCost: number
  overallWastePercentage: number
  weeklyBudget: number | null
  purchaseBudgetExceeded: boolean
  budgetCalculationComplete: boolean
  calculationComplete: boolean
  warningCount: number
  demoData: boolean
}

export type ShoppingListPage = PageResponse<ShoppingListSummary>

export interface ShoppingListFilters {
  supermarketCode?: string
  status?: ShoppingListStatus
  generatedFrom?: string
  generatedTo?: string
  calculationComplete?: string
  budgetExceeded?: string
  page: number
  size: number
  sort: string
}
