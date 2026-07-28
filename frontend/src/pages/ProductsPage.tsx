import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SiteHeader } from '../components/SiteHeader'
import { ProductCard } from '../features/catalog/ProductCard'
import {
  useAllergens,
  useCategories,
  useDietaryTags,
  useProducts,
  useSupermarkets,
} from '../hooks/useCatalogQueries'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import type { CatalogOption, ProductFilters } from '../types/api'
import styles from './ProductsPage.module.css'

const DEFAULT_SUPERMARKET = 'MERCADONA'
const PAGE_SIZE = 12

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryFromUrl = searchParams.get('query') ?? ''
  const [searchText, setSearchText] = useState(queryFromUrl)
  const debouncedSearch = useDebouncedValue(searchText, 400)
  const supermarketCode = searchParams.get('supermarket') ?? DEFAULT_SUPERMARKET

  const supermarkets = useSupermarkets()
  const categories = useCategories(supermarketCode)
  const dietaryTags = useDietaryTags()
  const allergens = useAllergens()

  useEffect(() => {
    if (debouncedSearch.trim() !== searchText.trim() || debouncedSearch.trim() === queryFromUrl) {
      return
    }
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current)
        if (debouncedSearch.trim()) {
          next.set('query', debouncedSearch.trim())
        } else {
          next.delete('query')
        }
        next.delete('page')
        return next
      },
      { replace: true },
    )
  }, [debouncedSearch, queryFromUrl, searchText, setSearchParams])

  const filters = useMemo<ProductFilters>(
    () => ({
      supermarketCode,
      categoryId: optional(searchParams.get('category')),
      query: optional(queryFromUrl),
      available: parseBoolean(searchParams.get('available')),
      maximumPrice: optional(searchParams.get('maximumPrice')),
      maximumCalories: optional(searchParams.get('maximumCalories')),
      minimumProtein: optional(searchParams.get('minimumProtein')),
      dietaryTags: parseList(searchParams.get('tags')),
      excludedAllergens: parseList(searchParams.get('exclude')),
      page: parsePage(searchParams.get('page')),
      size: PAGE_SIZE,
      sort: searchParams.get('sort') ?? 'name,asc',
    }),
    [queryFromUrl, searchParams, supermarketCode],
  )
  const products = useProducts(filters)

  function updateParam(key: string, value?: string) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      if (value) {
        next.set(key, value)
      } else {
        next.delete(key)
      }
      if (key !== 'page') {
        next.delete('page')
      }
      return next
    })
  }

  function toggleCode(key: 'tags' | 'exclude', code: string) {
    const selected = new Set(parseList(searchParams.get(key)))
    if (selected.has(code)) {
      selected.delete(code)
    } else {
      selected.add(code)
    }
    updateParam(key, [...selected].join(','))
  }

  function clearFilters() {
    setSearchText('')
    setSearchParams({ supermarket: DEFAULT_SUPERMARKET })
  }

  const page = products.data?.page ?? filters.page
  const totalPages = products.data?.totalPages ?? 0

  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.main}>
        <section className={styles.intro}>
          <div>
            <span>FASE 1 · Catálogo completo</span>
            <h1>Encuentra productos que encajen contigo</h1>
            <p>
              Combina precio, nutrición, disponibilidad, etiquetas y alérgenos sobre un catálogo
              completamente ficticio.
            </p>
          </div>
          <aside>
            <strong>Datos de demostración</strong>
            <span>Los precios no son reales ni actuales.</span>
          </aside>
        </section>

        <div className={styles.catalogLayout}>
          <aside className={styles.filters} aria-label="Filtros del catálogo">
            <div className={styles.filterHeading}>
              <div>
                <span>Refina resultados</span>
                <h2>Filtros</h2>
              </div>
              <button type="button" onClick={clearFilters}>
                Limpiar filtros
              </button>
            </div>

            <label className={styles.field}>
              <span>Supermercado</span>
              <select
                value={supermarketCode}
                onChange={(event) => updateParam('supermarket', event.target.value)}
              >
                {supermarkets.data?.map((supermarket) => (
                  <option
                    key={supermarket.code}
                    value={supermarket.code}
                    disabled={!supermarket.enabled}
                  >
                    {supermarket.name}
                    {!supermarket.enabled ? ' (próximamente)' : ''}
                  </option>
                ))}
              </select>
            </label>

            <label className={styles.field}>
              <span>Buscar por nombre o marca</span>
              <input
                type="search"
                value={searchText}
                onChange={(event) => setSearchText(event.target.value)}
                placeholder="Ej. pollo"
              />
            </label>

            <label className={styles.field}>
              <span>Categoría</span>
              <select
                value={searchParams.get('category') ?? ''}
                onChange={(event) => updateParam('category', event.target.value)}
              >
                <option value="">Todas las categorías</option>
                {categories.data?.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>

            <label className={styles.field}>
              <span>Disponibilidad</span>
              <select
                value={searchParams.get('available') ?? ''}
                onChange={(event) => updateParam('available', event.target.value)}
              >
                <option value="">Cualquier estado</option>
                <option value="true">Disponible</option>
                <option value="false">No disponible</option>
              </select>
            </label>

            <div className={styles.numericGrid}>
              <NumberFilter
                label="Precio máximo (€)"
                value={searchParams.get('maximumPrice') ?? ''}
                onChange={(value) => updateParam('maximumPrice', value)}
              />
              <NumberFilter
                label="Calorías máximas"
                value={searchParams.get('maximumCalories') ?? ''}
                onChange={(value) => updateParam('maximumCalories', value)}
              />
              <NumberFilter
                label="Proteína mínima (g)"
                value={searchParams.get('minimumProtein') ?? ''}
                onChange={(value) => updateParam('minimumProtein', value)}
              />
            </div>

            <OptionCheckboxes
              legend="Etiquetas dietéticas"
              options={dietaryTags.data ?? []}
              selected={parseList(searchParams.get('tags'))}
              onToggle={(code) => toggleCode('tags', code)}
            />

            <OptionCheckboxes
              legend="Alérgenos a excluir"
              options={allergens.data ?? []}
              selected={parseList(searchParams.get('exclude'))}
              onToggle={(code) => toggleCode('exclude', code)}
            />
          </aside>

          <section className={styles.results} aria-labelledby="results-heading">
            <div className={styles.resultsHeader}>
              <div>
                <span>Catálogo ficticio</span>
                <h2 id="results-heading">
                  {products.data ? `${products.data.totalElements} resultados` : 'Productos'}
                </h2>
              </div>
              <label>
                <span>Ordenar por</span>
                <select
                  value={filters.sort}
                  onChange={(event) => updateParam('sort', event.target.value)}
                >
                  <option value="name,asc">Nombre A–Z</option>
                  <option value="name,desc">Nombre Z–A</option>
                  <option value="currentPrice,asc">Precio más bajo</option>
                  <option value="currentPrice,desc">Precio más alto</option>
                  <option value="unitPrice,asc">Precio unitario más bajo</option>
                  <option value="lastSyncedAt,desc">Actualización más reciente</option>
                </select>
              </label>
            </div>

            {products.isPending ? (
              <CatalogState message="Cargando catálogo…" />
            ) : products.isError ? (
              <CatalogState
                error
                message="No se ha podido cargar el catálogo. Revisa el backend e inténtalo de nuevo."
              />
            ) : products.data.content.length === 0 ? (
              <CatalogState message="No hay productos para esta combinación de filtros." />
            ) : (
              <>
                {products.isFetching && (
                  <p className={styles.refreshing} aria-live="polite">
                    Actualizando resultados…
                  </p>
                )}
                <div className={styles.grid}>
                  {products.data.content.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>
                <nav className={styles.pagination} aria-label="Paginación del catálogo">
                  <button
                    type="button"
                    disabled={page === 0}
                    onClick={() => updateParam('page', String(page - 1))}
                  >
                    Anterior
                  </button>
                  <span>
                    Página {page + 1} de {Math.max(totalPages, 1)}
                  </span>
                  <button
                    type="button"
                    disabled={page + 1 >= totalPages}
                    onClick={() => updateParam('page', String(page + 1))}
                  >
                    Siguiente
                  </button>
                </nav>
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}

function NumberFilter({
  label,
  value,
  onChange,
}: {
  label: string
  value: string
  onChange: (value: string) => void
}) {
  return (
    <label className={styles.field}>
      <span>{label}</span>
      <input
        type="number"
        min="0"
        step="0.1"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Sin límite"
      />
    </label>
  )
}

function OptionCheckboxes({
  legend,
  options,
  selected,
  onToggle,
}: {
  legend: string
  options: CatalogOption[]
  selected: string[]
  onToggle: (code: string) => void
}) {
  return (
    <fieldset className={styles.checkGroup}>
      <legend>{legend}</legend>
      <div>
        {options.map((option) => (
          <label key={option.code}>
            <input
              type="checkbox"
              checked={selected.includes(option.code)}
              onChange={() => onToggle(option.code)}
            />
            <span>{option.name}</span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}

function CatalogState({ message, error = false }: { message: string; error?: boolean }) {
  return (
    <div
      className={`${styles.state} ${error ? styles.error : ''}`}
      role={error ? 'alert' : 'status'}
    >
      <span aria-hidden="true">{error ? '!' : 'S'}</span>
      <p>{message}</p>
    </div>
  )
}

function optional(value: string | null): string | undefined {
  return value || undefined
}

function parseBoolean(value: string | null): boolean | undefined {
  return value === 'true' ? true : value === 'false' ? false : undefined
}

function parseList(value: string | null): string[] {
  return value ? value.split(',').filter(Boolean) : []
}

function parsePage(value: string | null): number {
  const page = Number(value ?? '0')
  return Number.isInteger(page) && page >= 0 ? page : 0
}
