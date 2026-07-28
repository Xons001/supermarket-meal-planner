import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getAllergens, getCategories, getDietaryTags } from '../api/catalogMetadata'
import { getBackendHealth } from '../api/health'
import { getPriceHistory, getProduct, getProducts } from '../api/products'
import { getSupermarkets } from '../api/supermarkets'
import type { ProductFilters } from '../types/api'

export function useBackendHealth() {
  return useQuery({
    queryKey: ['backend-health'],
    queryFn: getBackendHealth,
    refetchInterval: 30_000,
    retry: 1,
  })
}

export function useSupermarkets() {
  return useQuery({
    queryKey: ['supermarkets'],
    queryFn: getSupermarkets,
  })
}

export function useProducts(filters: ProductFilters, enabled = true) {
  return useQuery({
    queryKey: ['products', filters],
    queryFn: () => getProducts(filters),
    placeholderData: keepPreviousData,
    enabled,
  })
}

export function useCategories(supermarketCode?: string) {
  return useQuery({
    queryKey: ['categories', supermarketCode],
    queryFn: () => getCategories(supermarketCode),
  })
}

export function useDietaryTags() {
  return useQuery({
    queryKey: ['dietary-tags'],
    queryFn: getDietaryTags,
  })
}

export function useAllergens() {
  return useQuery({
    queryKey: ['allergens'],
    queryFn: getAllergens,
  })
}

export function useProduct(productId?: string) {
  return useQuery({
    queryKey: ['product', productId],
    queryFn: () => getProduct(productId ?? ''),
    enabled: Boolean(productId),
  })
}

export function usePriceHistory(productId?: string) {
  return useQuery({
    queryKey: ['price-history', productId],
    queryFn: () => getPriceHistory(productId ?? ''),
    enabled: Boolean(productId),
  })
}
