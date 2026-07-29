import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { SiteHeader } from '../components/SiteHeader'
import { MealTemplateCard } from '../features/mealTemplates/MealTemplateCard'
import { useAllergens, useDietaryTags, useSupermarkets } from '../hooks/useCatalogQueries'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useMealTemplates } from '../hooks/useMealTemplateQueries'
import type { CatalogOption, MealTemplateFilters, MealType } from '../types/api'
import styles from './MealTemplatesPage.module.css'
import { useAuth } from '../auth/AuthProvider'

const DEFAULT_SUPERMARKET = 'MERCADONA'
const PAGE_SIZE = 9

export function MealTemplatesPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const queryFromUrl = searchParams.get('query') ?? ''
  const [searchText, setSearchText] = useState(queryFromUrl)
  const debouncedSearch = useDebouncedValue(searchText, 400)
  const supermarketCode = searchParams.get('supermarket') ?? DEFAULT_SUPERMARKET
  const supermarkets = useSupermarkets()
  const dietaryTags = useDietaryTags()
  const allergens = useAllergens()

  useEffect(() => {
    if (debouncedSearch.trim() !== searchText.trim() || debouncedSearch.trim() === queryFromUrl) {
      return
    }
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current)
        setOrDelete(next, 'query', debouncedSearch.trim())
        next.delete('page')
        return next
      },
      { replace: true },
    )
  }, [debouncedSearch, queryFromUrl, searchText, setSearchParams])

  const filters = useMemo<MealTemplateFilters>(
    () => ({
      supermarketCode,
      mealType: optional(searchParams.get('mealType')) as MealType | undefined,
      active: parseBoolean(searchParams.get('active')),
      query: optional(queryFromUrl),
      minimumProtein: optional(searchParams.get('minimumProtein')),
      maximumCalories: optional(searchParams.get('maximumCalories')),
      maximumPreparationMinutes: optional(searchParams.get('maximumPreparationMinutes')),
      dietaryTags: parseList(searchParams.get('tags')),
      excludedAllergens: parseList(searchParams.get('exclude')),
      page: parsePage(searchParams.get('page')),
      size: PAGE_SIZE,
      sort: searchParams.get('sort') ?? 'name,asc',
    }),
    [queryFromUrl, searchParams, supermarketCode],
  )
  const templates = useMealTemplates(filters)

  function updateParam(key: string, value?: string) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      setOrDelete(next, key, value)
      if (key !== 'page') next.delete('page')
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

  const page = templates.data?.page ?? filters.page
  const totalPages = templates.data?.totalPages ?? 0

  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.main}>
        <section className={styles.intro}>
          <div>
            <span>FASE 2 · Plantillas reutilizables</span>
            <h1>Comidas calculadas ingrediente a ingrediente</h1>
            <p>
              Explora preparaciones ficticias con nutrición, coste consumido, raciones y
              advertencias verificables.
            </p>
          </div>
          {user?.role === 'ADMIN' && (
            <Link className={styles.createButton} to="/meal-templates/new">
              Crear plantilla
            </Link>
          )}
        </section>

        <div className={styles.layout}>
          <aside className={styles.filters} aria-label="Filtros de plantillas">
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
              <span>Buscar por nombre o descripción</span>
              <input
                type="search"
                value={searchText}
                onChange={(event) => setSearchText(event.target.value)}
                placeholder="Ej. pollo"
              />
            </label>
            <label className={styles.field}>
              <span>Tipo de comida</span>
              <select
                value={searchParams.get('mealType') ?? ''}
                onChange={(event) => updateParam('mealType', event.target.value)}
              >
                <option value="">Todos</option>
                <option value="BREAKFAST">Desayuno</option>
                <option value="LUNCH">Comida</option>
                <option value="SNACK">Merienda</option>
                <option value="DINNER">Cena</option>
              </select>
            </label>
            <label className={styles.field}>
              <span>Estado</span>
              <select
                value={searchParams.get('active') ?? ''}
                onChange={(event) => updateParam('active', event.target.value)}
              >
                <option value="">Cualquier estado</option>
                <option value="true">Activas</option>
                <option value="false">Desactivadas</option>
              </select>
            </label>
            <NumberFilter
              label="Proteína mínima por ración"
              value={searchParams.get('minimumProtein') ?? ''}
              onChange={(value) => updateParam('minimumProtein', value)}
            />
            <NumberFilter
              label="Calorías máximas por ración"
              value={searchParams.get('maximumCalories') ?? ''}
              onChange={(value) => updateParam('maximumCalories', value)}
            />
            <NumberFilter
              label="Tiempo máximo (min)"
              value={searchParams.get('maximumPreparationMinutes') ?? ''}
              onChange={(value) => updateParam('maximumPreparationMinutes', value)}
            />
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

          <section className={styles.results} aria-labelledby="meal-results-heading">
            <div className={styles.resultsHeader}>
              <div>
                <span>Datos de demostración</span>
                <h2 id="meal-results-heading">
                  {templates.data ? `${templates.data.totalElements} plantillas` : 'Plantillas'}
                </h2>
              </div>
              <label>
                <span>Ordenar por</span>
                <select
                  value={filters.sort}
                  onChange={(event) => updateParam('sort', event.target.value)}
                >
                  <option value="name,asc">Nombre A–Z</option>
                  <option value="preparationMinutes,asc">Menos tiempo</option>
                  <option value="caloriesPerServing,asc">Menos calorías</option>
                  <option value="proteinPerServing,desc">Más proteína</option>
                  <option value="costPerServing,asc">Menor coste consumido</option>
                  <option value="updatedAt,desc">Actualización reciente</option>
                </select>
              </label>
            </div>

            {templates.isPending ? (
              <PageState message="Cargando plantillas…" />
            ) : templates.isError ? (
              <PageState error message="No se han podido cargar las plantillas." />
            ) : templates.data.content.length === 0 ? (
              <PageState message="No hay plantillas para esta combinación de filtros." />
            ) : (
              <>
                {templates.isFetching && (
                  <p className={styles.refreshing} aria-live="polite">
                    Actualizando resultados…
                  </p>
                )}
                <div className={styles.grid}>
                  {templates.data.content.map((template) => (
                    <MealTemplateCard key={template.id} template={template} />
                  ))}
                </div>
                <nav className={styles.pagination} aria-label="Paginación de plantillas">
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

function PageState({ message, error = false }: { message: string; error?: boolean }) {
  return (
    <div
      className={`${styles.state} ${error ? styles.error : ''}`}
      role={error ? 'alert' : 'status'}
    >
      <p>{message}</p>
    </div>
  )
}

function setOrDelete(params: URLSearchParams, key: string, value?: string): void {
  if (value) {
    params.set(key, value)
  } else {
    params.delete(key)
  }
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
