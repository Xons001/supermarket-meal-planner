import type { MealType, NutritionBreakdown, PageResponse } from './api'

export type MealPlanStatus = 'DRAFT' | 'GENERATED' | 'ARCHIVED'
export type VarietyPreference = 'LOW' | 'MEDIUM' | 'HIGH'

export interface GenerateMealPlanRequest {
  supermarketCode: string
  name: string
  startDate: string
  numberOfDays: number
  mealsPerDay: number
  servings: number
  dailyCaloriesTarget: number
  dailyProteinTarget: number
  weeklyBudget?: number
  allowedMealTypes: MealType[]
  requiredDietaryTags: string[]
  excludedAllergens: string[]
  excludedTemplateIds: string[]
  excludedProductIds: string[]
  maximumPreparationMinutes?: number
  maximumTemplateRepetitions?: number
  varietyPreference: VarietyPreference
  allowIncompleteCalculations: boolean
  deterministicSeed?: number
  generationToken?: string
  persist: boolean
}

export interface MealPlanWarning {
  code: string
  message: string
  severity: 'INFO' | 'WARNING' | 'ERROR'
  dayIndex: number | null
}

export interface PlannedMeal {
  position: number
  mealType: MealType
  templateId: string
  templateName: string
  servings: number
  preparationMinutes: number
  ingredients: {
    productId: string
    productName: string
    quantity: number
    quantityUnit: string
  }[]
  nutrition: NutritionBreakdown
  consumedCost: number
  score: number
  calculationComplete: boolean
  warnings: string[]
}

export interface MealPlanDay {
  dayIndex: number
  date: string
  meals: PlannedMeal[]
  totalNutrition: NutritionBreakdown
  totalConsumedCost: number
  calorieTarget: number
  proteinTarget: number
  calorieDeviation: number
  calorieDeviationPercentage: number
  proteinDeviation: number
  dailyScore: number
  warnings: MealPlanWarning[]
}

export interface GeneratedMealPlan {
  persisted: boolean
  mealPlanId: string | null
  generationToken: string
  name: string
  supermarketCode: string
  supermarketName: string
  startDate: string
  numberOfDays: number
  mealsPerDay: number
  servings: number
  seed: number
  strategy: 'SCORING'
  status: MealPlanStatus
  criteria: {
    dailyCaloriesTarget: number
    dailyProteinTarget: number
    allowedMealTypes: MealType[]
    requiredDietaryTags: string[]
    excludedAllergens: string[]
    excludedTemplateIds: string[]
    excludedProductIds: string[]
    maximumPreparationMinutes: number | null
    maximumTemplateRepetitions: number
    varietyPreference: VarietyPreference
    allowIncompleteCalculations: boolean
  }
  days: MealPlanDay[]
  weeklyNutrition: NutritionBreakdown
  totalConsumedCost: number
  weeklyBudget: number | null
  budgetDifference: number | null
  budgetExceeded: boolean
  budgetDeviationPercentage: number | null
  overallScore: number
  scoreBreakdown: {
    calorieScore: number
    proteinScore: number
    budgetScore: number
    varietyScore: number
    repetitionScore: number
    completenessScore: number
    preparationScore: number
    totalScore: number
  }
  varietyMetrics: {
    uniqueTemplates: number
    repeatedTemplates: number
    maximumObservedRepetition: number
    varietyScore: number
  }
  calculationComplete: boolean
  warnings: MealPlanWarning[]
  constraintsApplied: string[]
  constraintsNotMet: string[]
  rejectedCandidateStatistics: Record<string, number>
  generationMetadata: {
    strategy: 'SCORING'
    seed: number
    durationMilliseconds: number
    candidatesEvaluated: number
    completePlansEvaluated: number
    generatedAt: string
    algorithmVersion: string
    beamWidth: number
    candidatesPerPosition: number
  }
  createdAt: string | null
  updatedAt: string | null
}

export interface MealPlanSummary {
  id: string
  name: string
  supermarketCode: string
  supermarketName: string
  startDate: string
  numberOfDays: number
  mealsPerDay: number
  servings: number
  dailyCaloriesTarget: number
  dailyProteinTarget: number
  totalConsumedCost: number
  weeklyBudget: number | null
  overallScore: number
  status: MealPlanStatus
  calculationComplete: boolean
  warningCount: number
  seed: number
  createdAt: string
  updatedAt: string
}

export interface MealPlanFilters {
  supermarketCode?: string
  status?: MealPlanStatus
  startDateFrom?: string
  startDateTo?: string
  minimumScore?: string
  page: number
  size: number
  sort: string
}

export type MealPlanPage = PageResponse<MealPlanSummary>
