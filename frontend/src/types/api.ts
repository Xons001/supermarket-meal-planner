export interface Supermarket {
  code: string
  name: string
  enabled: boolean
  catalogSource: string
  countryCode: string
  currencyCode: string
}

export interface Nutrition {
  caloriesPer100g: number
  proteinPer100g: number
  carbohydratesPer100g: number
  fatPer100g: number
  fiberPer100g: number
  sugarPer100g: number
  saltPer100g: number
  perUnit: NutritionBreakdown | null
  dataSource: string
  verificationStatus: string
  confidenceScore: number
  updatedAt: string
}

export interface DietaryTag {
  id: string
  code: string
  name: string
}

export interface Allergen extends DietaryTag {
  presenceType: 'CONTAINS' | 'MAY_CONTAIN' | 'TRACES' | 'UNKNOWN'
}

export interface Product {
  id: string
  supermarketCode: string
  supermarketName: string
  categoryId: string
  categoryName: string
  externalId: string
  barcode: string | null
  name: string
  brand: string | null
  description: string | null
  imageUrl: string | null
  currentPrice: number
  unitPrice: number
  packageQuantity: number
  packageUnit: string
  measurementType: 'WEIGHT' | 'VOLUME' | 'UNIT'
  costDataComplete: boolean
  available: boolean
  source: string
  lastSyncedAt: string
  nutrition: Nutrition | null
  dietaryTags: DietaryTag[]
  allergens: Allergen[]
  demonstrationData: boolean
}

export interface Category {
  id: string
  externalId: string
  name: string
  parentCategoryId: string | null
  supermarketCode: string
}

export interface CatalogOption {
  id: string
  code: string
  name: string
}

export interface PriceHistoryEntry {
  id: string
  price: number
  unitPrice: number
  recordedAt: string
  demonstrationData: boolean
}

export interface ProductFilters {
  supermarketCode?: string
  categoryId?: string
  query?: string
  available?: boolean
  maximumPrice?: string
  maximumCalories?: string
  minimumProtein?: string
  dietaryTags?: string[]
  excludedAllergens?: string[]
  page: number
  size: number
  sort: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface HealthResponse {
  status: string
  components?: Record<string, unknown>
}

export type MealType = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'DINNER'
export type QuantityUnit = 'GRAM' | 'MILLILITER' | 'UNIT'

export interface NutritionBreakdown {
  calories: number
  protein: number
  carbohydrates: number
  fat: number
  fiber: number
  sugar: number
  salt: number
}

export interface MealTemplateIngredient {
  productId: string
  productName: string
  brand: string | null
  category: string
  quantity: number
  quantityUnit: QuantityUnit
  optional: boolean
  sortOrder: number
  notes: string | null
  calculatedNutrition: NutritionBreakdown | null
  calculatedConsumedCost: number | null
  nutritionCalculationComplete: boolean
  costCalculationComplete: boolean
  calculationComplete: boolean
  warnings: string[]
}

export interface MealTemplate {
  id: string
  supermarketCode: string
  supermarketName: string
  name: string
  description: string
  mealType: MealType
  instructions: string[]
  preparationMinutes: number
  servings: number
  active: boolean
  imageUrl: string | null
  ingredients: MealTemplateIngredient[]
  totalNutrition: NutritionBreakdown
  nutritionPerServing: NutritionBreakdown
  totalConsumedCost: number
  consumedCostPerServing: number
  calculationComplete: boolean
  nutritionComplete: boolean
  costComplete: boolean
  warnings: string[]
  createdAt: string
  updatedAt: string
  demoData: boolean
}

export interface MealTemplateIngredientRequest {
  productId: string
  quantity: number
  quantityUnit: QuantityUnit
  optional: boolean
  sortOrder: number
  notes?: string | null
}

export interface MealTemplateRequest {
  supermarketCode: string
  name: string
  description: string
  mealType: MealType
  instructions: string[]
  preparationMinutes: number
  servings: number
  active: boolean
  imageUrl?: string | null
  ingredients: MealTemplateIngredientRequest[]
}

export interface MealTemplateFilters {
  supermarketCode?: string
  mealType?: MealType
  active?: boolean
  query?: string
  minimumProtein?: string
  maximumCalories?: string
  maximumPreparationMinutes?: string
  excludedAllergens?: string[]
  dietaryTags?: string[]
  page: number
  size: number
  sort: string
}

export interface ProblemDetails {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errorCode?: string
}
