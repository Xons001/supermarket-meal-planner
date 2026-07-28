import { useQuery } from '@tanstack/react-query'
import { getBackendHealth } from '../api/health'
import { getProducts } from '../api/products'
import { getSupermarkets } from '../api/supermarkets'

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

export function useProducts(supermarketCode: string | undefined) {
  return useQuery({
    queryKey: ['products', supermarketCode],
    queryFn: () => getProducts(supermarketCode ?? ''),
    enabled: Boolean(supermarketCode),
  })
}
