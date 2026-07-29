import { Link, useSearchParams } from 'react-router'
import { SiteHeader } from '../components/SiteHeader'
import { useSupermarkets } from '../hooks/useCatalogQueries'
import { useShoppingLists } from '../hooks/useShoppingListQueries'
import type { ShoppingListFilters, ShoppingListStatus } from '../types/shoppingList'
import { formatDate, formatDecimal, formatMoney } from '../utils/format'
import styles from './ShoppingListsPage.module.css'

export function ShoppingListsPage() {
  const [params, setParams] = useSearchParams()
  const filters: ShoppingListFilters = {
    supermarketCode: params.get('supermarketCode') || undefined,
    status: (params.get('status') || undefined) as ShoppingListStatus | undefined,
    generatedFrom: params.get('generatedFrom') || undefined,
    generatedTo: params.get('generatedTo') || undefined,
    calculationComplete: params.get('calculationComplete') || undefined,
    budgetExceeded: params.get('budgetExceeded') || undefined,
    page: validPage(params.get('page')),
    size: 9,
    sort: params.get('sort') ?? 'generatedAt,desc',
  }
  const lists = useShoppingLists(filters)
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
            <span>FASE 4 · Paquetes completos</span>
            <h1>Listas de compra</h1>
            <p>Coste real estimado, paquetes necesarios y sobrantes agrupados por plan.</p>
          </div>
          <strong>Datos de demostración · El precio puede variar</strong>
        </header>

        <section className={styles.filters} aria-label="Filtros de listas de compra">
          <label>
            Supermercado
            <select
              value={filters.supermarketCode ?? ''}
              onChange={(event) => update('supermarketCode', event.target.value)}
            >
              <option value="">Todos</option>
              {supermarkets.data?.map((market) => (
                <option value={market.code} key={market.code}>
                  {market.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Estado
            <select
              value={filters.status ?? ''}
              onChange={(event) => update('status', event.target.value)}
            >
              <option value="">Todos</option>
              <option value="GENERATED">Generada</option>
              <option value="ARCHIVED">Archivada</option>
            </select>
          </label>
          <label>
            Generada desde
            <input
              type="date"
              value={filters.generatedFrom ?? ''}
              onChange={(event) => update('generatedFrom', event.target.value)}
            />
          </label>
          <label>
            Generada hasta
            <input
              type="date"
              value={filters.generatedTo ?? ''}
              onChange={(event) => update('generatedTo', event.target.value)}
            />
          </label>
          <label>
            Cálculo
            <select
              value={filters.calculationComplete ?? ''}
              onChange={(event) => update('calculationComplete', event.target.value)}
            >
              <option value="">Todos</option>
              <option value="true">Completo</option>
              <option value="false">Parcial</option>
            </select>
          </label>
          <label>
            Presupuesto
            <select
              value={filters.budgetExceeded ?? ''}
              onChange={(event) => update('budgetExceeded', event.target.value)}
            >
              <option value="">Todos</option>
              <option value="true">Superado</option>
              <option value="false">No superado</option>
            </select>
          </label>
          <label>
            Orden
            <select value={filters.sort} onChange={(event) => update('sort', event.target.value)}>
              <option value="generatedAt,desc">Más recientes</option>
              <option value="totalPurchaseCost,desc">Mayor coste real</option>
              <option value="totalPurchaseCost,asc">Menor coste real</option>
              <option value="totalWasteCost,desc">Mayor coste sobrante</option>
              <option value="overallWastePercentage,desc">Mayor desperdicio</option>
            </select>
          </label>
        </section>

        {lists.isPending ? (
          <PageState text="Cargando listas de compra…" />
        ) : lists.isError ? (
          <PageState text="No se han podido cargar las listas." error />
        ) : lists.data.content.length === 0 ? (
          <PageState text="No hay listas de compra para estos filtros." />
        ) : (
          <>
            <section className={styles.grid} aria-label="Listas de compra">
              {lists.data.content.map((list) => (
                <article key={list.id}>
                  <div className={styles.cardTop}>
                    <span data-status={list.status}>
                      {list.status === 'ARCHIVED' ? 'Archivada' : 'Generada'}
                    </span>
                    <strong>
                      {list.calculationComplete ? 'Cálculo completo' : 'Cálculo parcial'}
                    </strong>
                  </div>
                  <h2>
                    <Link to={`/shopping-lists/${list.id}`}>{list.mealPlanName}</Link>
                  </h2>
                  <p>
                    {list.supermarketName} · {formatDate(list.generatedAt)}
                  </p>
                  <dl>
                    <Metric label="Productos" value={String(list.itemCount)} />
                    <Metric label="Paquetes" value={String(list.totalPackages)} />
                    <Metric label="Coste consumido" value={formatMoney(list.totalConsumedCost)} />
                    <Metric label="Coste real" value={formatMoney(list.totalPurchaseCost)} />
                    <Metric label="Coste sobrante" value={formatMoney(list.totalWasteCost)} />
                    <Metric
                      label="Desperdicio"
                      value={`${formatDecimal(list.overallWastePercentage)} %`}
                    />
                  </dl>
                  <div className={styles.links}>
                    <Link to={`/shopping-lists/${list.id}`}>Abrir lista</Link>
                    <Link to={`/meal-plans/${list.mealPlanId}`}>Ver plan</Link>
                  </div>
                </article>
              ))}
            </section>
            <nav className={styles.pagination} aria-label="Paginación de listas">
              <button
                disabled={lists.data.first}
                onClick={() => update('page', String(filters.page - 1))}
              >
                Anterior
              </button>
              <span>
                Página {lists.data.page + 1} de {Math.max(1, lists.data.totalPages)}
              </span>
              <button
                disabled={lists.data.last}
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

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
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
