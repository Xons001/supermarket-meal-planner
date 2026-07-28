import { apiFetch } from './client'
import type { PageResponse, PriceHistoryEntry, Product, ProductFilters } from '../types/api'

export function getProducts(filters: ProductFilters): Promise<PageResponse<Product>> {
  const query = new URLSearchParams()
  append(query, 'supermarketCode', filters.supermarketCode)
  append(query, 'categoryId', filters.categoryId)
  append(query, 'query', filters.query)
  if (filters.available !== undefined) {
    query.set('available', String(filters.available))
  }
  append(query, 'maximumPrice', filters.maximumPrice)
  append(query, 'maximumCalories', filters.maximumCalories)
  append(query, 'minimumProtein', filters.minimumProtein)
  if (filters.dietaryTags?.length) {
    query.set('dietaryTags', filters.dietaryTags.join(','))
  }
  if (filters.excludedAllergens?.length) {
    query.set('excludedAllergens', filters.excludedAllergens.join(','))
  }
  query.set('page', String(filters.page))
  query.set('size', String(filters.size))
  query.set('sort', filters.sort)

  return apiFetch<PageResponse<Product>>(`/api/v1/products?${query.toString()}`)
}

export function getProduct(productId: string): Promise<Product> {
  return apiFetch<Product>(`/api/v1/products/${productId}`)
}

export function getPriceHistory(productId: string): Promise<PriceHistoryEntry[]> {
  return apiFetch<PriceHistoryEntry[]>(`/api/v1/products/${productId}/price-history`)
}

function append(query: URLSearchParams, key: string, value: string | undefined) {
  if (value) {
    query.set(key, value)
  }
}
