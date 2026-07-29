export type OptimizationPreset = 'BALANCED' | 'LOWER_PURCHASE_COST' | 'LOWER_WASTE' | 'MORE_REUSE'
export type GenerationStrategy = 'PURCHASE_AWARE_SCORING' | 'SCORING'

export interface UserPreferences {
  dailyCaloriesTarget: number
  dailyProteinTarget: number
  weeklyBudget: number | null
  numberOfDays: number
  mealsPerDay: number
  strategy: GenerationStrategy
  optimizationPreset: OptimizationPreset | null
  dietaryRestrictions: string[]
  allergens: string[]
}

export interface AuthUser {
  id: string
  email: string
  displayName: string
  status: 'ACTIVE' | 'DISABLED'
  role: 'USER' | 'ADMIN'
  createdAt: string
  preferences: UserPreferences
}
