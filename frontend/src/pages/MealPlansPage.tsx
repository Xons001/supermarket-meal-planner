import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import {
  duplicateMealPlan,
  restoreMealPlan,
  setMealPlanFavorite,
  changeMealPlanStatus,
} from '../api/mealPlans'
import { queryKeys } from '../app/queryKeys'
import { SiteHeader } from '../components/SiteHeader'
import { Button, Modal, useToast } from '../components/ui'
import { useSupermarkets } from '../hooks/useCatalogQueries'
import { useMealPlans } from '../hooks/useMealPlanQueries'
import type {
  MealPlanFilters,
  MealPlanGenerationStrategy,
  MealPlanStatus,
  MealPlanSummary,
} from '../types/mealPlan'
import { formatDate, formatDecimal, formatMoney } from '../utils/format'
import styles from './MealPlansPage.module.css'

export function MealPlansPage() {
  const [params, setParams] = useSearchParams()
  const [duplicating, setDuplicating] = useState<MealPlanSummary | null>(null)
  const queryClient = useQueryClient()
  const notify = useToast()
  const filters: MealPlanFilters = {
    supermarketCode: params.get('supermarketCode') || undefined,
    status: (params.get('status') || undefined) as MealPlanStatus | undefined,
    startDateFrom: params.get('startDateFrom') || undefined,
    startDateTo: params.get('startDateTo') || undefined,
    minimumScore: params.get('minimumScore') || undefined,
    q: params.get('q') || undefined,
    strategy: (params.get('strategy') || undefined) as MealPlanGenerationStrategy | undefined,
    favorite: params.get('favorite') || undefined,
    archived: params.get('archived') || undefined,
    page: validPage(params.get('page')),
    size: 9,
    sort: params.get('sort') ?? 'createdAt,desc',
  }
  const plans = useMealPlans(filters)
  const supermarkets = useSupermarkets()
  const organize = useMutation({
    mutationFn: async (action: {
      plan: MealPlanSummary
      type: 'favorite' | 'archive' | 'restore'
    }) => {
      if (action.type === 'favorite') {
        return setMealPlanFavorite(action.plan.id, !action.plan.favorite)
      }
      return action.type === 'archive'
        ? changeMealPlanStatus(action.plan.id, 'ARCHIVED')
        : restoreMealPlan(action.plan.id)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['meal-plans'] })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
      void queryClient.invalidateQueries({ queryKey: ['activity'] })
      notify('Plan actualizado')
    },
    onError: () => notify('No se pudo actualizar el plan', 'error'),
  })
  const duplicate = useMutation({
    mutationFn: (value: { id: string; name: string; startDate: string }) =>
      duplicateMealPlan(value.id, value.name, value.startDate),
    onSuccess: () => {
      setDuplicating(null)
      void queryClient.invalidateQueries({ queryKey: ['meal-plans'] })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard })
      notify('Plan duplicado con sus snapshots históricos')
    },
    onError: () => notify('No se pudo duplicar el plan', 'error'),
  })

  function update(key: string, value: string) {
    setParams((current) => {
      const next = new URLSearchParams(current)
      if (value) next.set(key, value)
      else next.delete(key)
      if (key !== 'page') next.delete('page')
      return next
    })
  }

  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.main}>
        <header className={styles.intro}>
          <div>
            <span>FASE 3 · Historial</span>
            <h1>Planes guardados</h1>
            <p>
              Consulta snapshots reproducibles, su puntuación y las advertencias vigentes al
              generarlos.
            </p>
          </div>
          <Link to="/meal-plans/new">Crear plan</Link>
        </header>

        <section className={styles.filters} aria-label="Filtros de planes">
          <label>
            Buscar
            <input
              type="search"
              placeholder="Nombre o UUID"
              value={filters.q ?? ''}
              onChange={(e) => update('q', e.target.value)}
            />
          </label>
          <label>
            Estrategia
            <select
              value={filters.strategy ?? ''}
              onChange={(e) => update('strategy', e.target.value)}
            >
              <option value="">Todas</option>
              <option value="PURCHASE_AWARE_SCORING">Compra eficiente</option>
              <option value="SCORING">Clásica</option>
            </select>
          </label>
          <label>
            Favoritos
            <select
              value={filters.favorite ?? ''}
              onChange={(e) => update('favorite', e.target.value)}
            >
              <option value="">Todos</option>
              <option value="true">Solo favoritos</option>
              <option value="false">No favoritos</option>
            </select>
          </label>
          <label>
            Supermercado
            <select
              value={filters.supermarketCode ?? ''}
              onChange={(e) => update('supermarketCode', e.target.value)}
            >
              <option value="">Todos</option>
              {supermarkets.data?.map((market) => (
                <option key={market.code} value={market.code}>
                  {market.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Estado
            <select value={filters.status ?? ''} onChange={(e) => update('status', e.target.value)}>
              <option value="">Todos</option>
              <option value="DRAFT">Borrador</option>
              <option value="GENERATED">Generado</option>
              <option value="ARCHIVED">Archivado</option>
            </select>
          </label>
          <label>
            Desde
            <input
              type="date"
              value={filters.startDateFrom ?? ''}
              onChange={(e) => update('startDateFrom', e.target.value)}
            />
          </label>
          <label>
            Hasta
            <input
              type="date"
              value={filters.startDateTo ?? ''}
              onChange={(e) => update('startDateTo', e.target.value)}
            />
          </label>
          <label>
            Score mínimo
            <input
              type="number"
              min="0"
              max="100"
              value={filters.minimumScore ?? ''}
              onChange={(e) => update('minimumScore', e.target.value)}
            />
          </label>
          <label>
            Orden
            <select value={filters.sort} onChange={(e) => update('sort', e.target.value)}>
              <option value="createdAt,desc">Más recientes</option>
              <option value="startDate,asc">Fecha de inicio</option>
              <option value="overallScore,desc">Mejor score</option>
              <option value="totalConsumedCost,asc">Menor coste</option>
              <option value="estimatedPurchaseCost,asc">Menor compra real</option>
              <option value="estimatedWasteCost,asc">Menor desperdicio</option>
              <option value="name,asc">Nombre A–Z</option>
            </select>
          </label>
        </section>

        {plans.isPending ? (
          <PageState text="Cargando planes…" />
        ) : plans.isError ? (
          <PageState text="No se han podido cargar los planes." error />
        ) : plans.data.content.length === 0 ? (
          <PageState text="Todavía no hay planes guardados para estos filtros." />
        ) : (
          <>
            <section className={styles.grid} aria-label="Planes guardados">
              {plans.data.content.map((plan) => (
                <article key={plan.id}>
                  <div className={styles.cardTop}>
                    <span data-status={plan.status}>{statusLabel(plan.status)}</span>
                    <strong>{formatDecimal(plan.overallScore)} / 100</strong>
                  </div>
                  <h2>
                    <Link to={`/meal-plans/${plan.id}`}>{plan.name}</Link>
                  </h2>
                  <p>
                    {plan.numberOfDays} días · {plan.mealsPerDay} comidas/día · desde{' '}
                    {formatDate(plan.startDate)}
                  </p>
                  <dl>
                    <div>
                      <dt>Coste consumido</dt>
                      <dd>{formatMoney(plan.totalConsumedCost)}</dd>
                    </div>
                    <div>
                      <dt>Compra real</dt>
                      <dd>
                        {plan.estimatedPurchaseCost == null
                          ? 'Parcial'
                          : formatMoney(plan.estimatedPurchaseCost)}
                      </dd>
                    </div>
                    <div>
                      <dt>Advertencias</dt>
                      <dd>{plan.warningCount}</dd>
                    </div>
                    <div>
                      <dt>Seed</dt>
                      <dd>{plan.seed}</dd>
                    </div>
                  </dl>
                  <Link className={styles.open} to={`/meal-plans/${plan.id}`}>
                    Abrir detalle
                  </Link>
                  <div className={styles.links}>
                    <button
                      onClick={() => organize.mutate({ plan, type: 'favorite' })}
                      aria-label={plan.favorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
                    >
                      {plan.favorite ? '★ Favorito' : '☆ Favorito'}
                    </button>
                    <button onClick={() => setDuplicating(plan)}>Duplicar</button>
                    <button
                      onClick={() =>
                        organize.mutate({
                          plan,
                          type: plan.status === 'ARCHIVED' ? 'restore' : 'archive',
                        })
                      }
                    >
                      {plan.status === 'ARCHIVED' ? 'Restaurar' : 'Archivar'}
                    </button>
                  </div>
                </article>
              ))}
            </section>
            <nav className={styles.pagination} aria-label="Paginación de planes">
              <button
                disabled={plans.data.first}
                onClick={() => update('page', String(filters.page - 1))}
              >
                Anterior
              </button>
              <span>
                Página {plans.data.page + 1} de {Math.max(1, plans.data.totalPages)}
              </span>
              <button
                disabled={plans.data.last}
                onClick={() => update('page', String(filters.page + 1))}
              >
                Siguiente
              </button>
            </nav>
          </>
        )}
      </main>
      <Modal open={duplicating !== null} title="Duplicar plan" onClose={() => setDuplicating(null)}>
        {duplicating && (
          <form
            className={styles.duplicateForm}
            onSubmit={(event) => {
              event.preventDefault()
              const data = new FormData(event.currentTarget)
              duplicate.mutate({
                id: duplicating.id,
                name: String(data.get('name')),
                startDate: String(data.get('startDate')),
              })
            }}
          >
            <p>Se conservarán exactamente cantidades, precios, disponibilidad y semillas.</p>
            <label>
              Nombre
              <input
                name="name"
                required
                maxLength={180}
                defaultValue={`${duplicating.name} (copia)`}
              />
            </label>
            <label>
              Nueva fecha inicial
              <input name="startDate" type="date" required defaultValue={duplicating.startDate} />
            </label>
            <Button disabled={duplicate.isPending}>Confirmar duplicación</Button>
          </form>
        )}
      </Modal>
    </div>
  )
}

function PageState({ text, error = false }: { text: string; error?: boolean }) {
  return (
    <div
      className={`${styles.state} ${error ? styles.error : ''}`}
      role={error ? 'alert' : 'status'}
    >
      {text}
    </div>
  )
}

function validPage(value: string | null): number {
  const page = Number(value ?? 0)
  return Number.isInteger(page) && page >= 0 ? page : 0
}

function statusLabel(status: MealPlanStatus): string {
  return { DRAFT: 'Borrador', GENERATED: 'Generado', ARCHIVED: 'Archivado' }[status]
}
