import { Link, useSearchParams } from 'react-router-dom'
import { SiteHeader } from '../components/SiteHeader'
import { useSupermarkets } from '../hooks/useCatalogQueries'
import { useMealPlans } from '../hooks/useMealPlanQueries'
import type { MealPlanFilters, MealPlanStatus } from '../types/mealPlan'
import { formatDate, formatDecimal, formatMoney } from '../utils/format'
import styles from './MealPlansPage.module.css'

export function MealPlansPage() {
  const [params, setParams] = useSearchParams()
  const filters: MealPlanFilters = {
    supermarketCode: params.get('supermarketCode') || undefined,
    status: (params.get('status') || undefined) as MealPlanStatus | undefined,
    startDateFrom: params.get('startDateFrom') || undefined,
    startDateTo: params.get('startDateTo') || undefined,
    minimumScore: params.get('minimumScore') || undefined,
    page: validPage(params.get('page')),
    size: 9,
    sort: params.get('sort') ?? 'createdAt,desc',
  }
  const plans = useMealPlans(filters)
  const supermarkets = useSupermarkets()

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
                      <dt>Objetivo</dt>
                      <dd>{formatDecimal(plan.dailyCaloriesTarget)} kcal</dd>
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
