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
  dataSource: string
  verificationStatus: string
  confidenceScore: number
  updatedAt: string
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
  currentPrice: number
  unitPrice: number
  packageQuantity: number
  packageUnit: string
  available: boolean
  source: string
  lastSyncedAt: string
  nutrition: Nutrition | null
  demonstrationData: boolean
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

export interface ProblemDetails {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errorCode?: string
}
