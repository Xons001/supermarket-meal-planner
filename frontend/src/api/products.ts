import { apiFetch } from './client'
import type { PageResponse, Product } from '../types/api'

export function getProducts(supermarketCode: string): Promise<PageResponse<Product>> {
  const query = new URLSearchParams({
    supermarketCode,
    page: '0',
    size: '20',
  })
  return apiFetch<PageResponse<Product>>(`/api/v1/products?${query.toString()}`)
}

export function getProduct(productId: string): Promise<Product> {
  return apiFetch<Product>(`/api/v1/products/${productId}`)
}
