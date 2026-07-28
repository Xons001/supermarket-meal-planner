import { Link, useNavigate, useParams } from 'react-router-dom'
import { shoppingListCsvUrl } from '../api/shoppingLists'
import { SiteHeader } from '../components/SiteHeader'
import {
  useArchiveShoppingList,
  useRegenerateShoppingList,
  useShoppingList,
} from '../hooks/useShoppingListQueries'
import type { ShoppingListItem } from '../types/shoppingList'
import { formatDate, formatDecimal, formatMoney } from '../utils/format'
import styles from './ShoppingListDetailPage.module.css'

export function ShoppingListDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const list = useShoppingList(id)
  const mealPlanId = list.data?.mealPlanId ?? ''
  const regenerate = useRegenerateShoppingList(mealPlanId)
  const archive = useArchiveShoppingList(mealPlanId)

  async function regenerateList() {
    if (!window.confirm('¿Regenerar la lista usando el snapshot del plan?')) return
    const replacement = await regenerate.mutateAsync()
    navigate(`/shopping-lists/${replacement.id}`, { replace: true })
  }

  async function archiveList() {
    if (!window.confirm('¿Archivar esta lista de compra?')) return
    await archive.mutateAsync()
    navigate('/shopping-lists')
  }

  if (list.isPending)
    return (
      <Shell>
        <State text="Cargando lista de compra…" />
      </Shell>
    )
  if (list.isError)
    return (
      <Shell>
        <State text="No se ha encontrado la lista solicitada." error />
      </Shell>
    )

  return (
    <Shell>
      <header className={styles.heading}>
        <div>
          <span>{list.data.status} · Datos de demostración</span>
          <h1>{list.data.mealPlanName}</h1>
          <p>
            {list.data.supermarket.name} · generada {formatDate(list.data.generatedAt)} · El precio
            puede variar
          </p>
        </div>
        <div className={styles.actions}>
          <Link to={`/meal-plans/${list.data.mealPlanId}`}>Volver al plan</Link>
          <a href={shoppingListCsvUrl(list.data.id)} download>
            Exportar CSV
          </a>
          <button onClick={() => window.print()}>Imprimir</button>
          {list.data.status === 'GENERATED' && (
            <>
              <button onClick={() => void regenerateList()} disabled={regenerate.isPending}>
                Regenerar
              </button>
              <button onClick={() => void archiveList()} disabled={archive.isPending}>
                Archivar
              </button>
            </>
          )}
        </div>
      </header>

      <section className={styles.summary} aria-label="Resumen de la lista">
        <Metric label="Coste consumido" value={formatMoney(list.data.totalConsumedCost)} />
        <Metric label="Coste real de compra" value={formatMoney(list.data.totalPurchaseCost)} />
        <Metric label="Coste estimado de sobrantes" value={formatMoney(list.data.totalWasteCost)} />
        <Metric
          label="Desperdicio"
          value={`${formatDecimal(list.data.overallWastePercentage)} %`}
        />
        <Metric label="Productos" value={String(list.data.itemCount)} />
        <Metric label="Paquetes" value={String(list.data.totalPackages)} />
      </section>

      <section className={styles.budget} aria-label="Presupuesto real">
        <div>
          <strong>Presupuesto</strong>
          <span>
            {list.data.weeklyBudget == null
              ? 'Sin presupuesto'
              : formatMoney(list.data.weeklyBudget)}
          </span>
        </div>
        <div>
          <strong>Diferencia</strong>
          <span>
            {list.data.purchaseBudgetDifference == null
              ? 'No aplicable'
              : formatMoney(list.data.purchaseBudgetDifference)}
          </span>
        </div>
        <p>
          {list.data.purchaseBudgetExceeded ? '⚠ Presupuesto real superado.' : '✓ No superado.'}{' '}
          {!list.data.budgetCalculationComplete && 'Comparación parcial.'}
        </p>
      </section>

      {!list.data.calculationComplete && (
        <p className={styles.partial} role="status">
          ⚠ Cálculo parcial: los productos no calculables permanecen visibles y no se contabilizan
          como cero.
        </p>
      )}

      {list.data.warnings.length > 0 && (
        <section className={styles.warnings} aria-labelledby="shopping-warnings">
          <h2 id="shopping-warnings">Advertencias</h2>
          <ul>
            {list.data.warnings.map((warning, index) => (
              <li key={`${warning.code}-${index}`}>
                <strong>{warning.severity}</strong> {warning.message}
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className={styles.quantities} aria-labelledby="quantity-summary">
        <h2 id="quantity-summary">Resumen de cantidades</h2>
        <div>
          {Object.entries(list.data.quantitySummary).map(([type, summary]) => (
            <article key={type}>
              <strong>{measurementLabel(type)}</strong>
              <span>Requerido: {formatQuantity(summary.required, summary.unit)}</span>
              <span>Comprado: {formatQuantity(summary.purchased, summary.unit)}</span>
              <span>Sobrante: {formatQuantity(summary.leftover, summary.unit)}</span>
              {!summary.calculationComplete && <em>Cálculo parcial</em>}
            </article>
          ))}
        </div>
      </section>

      <section className={styles.groups} aria-label="Productos agrupados por categoría">
        {list.data.groups.map((group) => (
          <section key={group.categoryId ?? group.categoryName}>
            <header>
              <div>
                <h2>{group.categoryName}</h2>
                <span>{group.items.length} productos</span>
              </div>
              <p>
                Compra {formatMoney(group.subtotalPurchaseCost)} · consumido{' '}
                {formatMoney(group.subtotalConsumedCost)} · sobrante{' '}
                {formatMoney(group.subtotalWasteCost)}
                {!group.calculationComplete && ' · parcial'}
              </p>
            </header>
            <div className={styles.items}>
              {group.items.map((item) => (
                <ShoppingItemCard item={item} key={item.id} />
              ))}
            </div>
          </section>
        ))}
      </section>
    </Shell>
  )
}

function ShoppingItemCard({ item }: { item: ShoppingListItem }) {
  return (
    <article className={styles.item}>
      <header>
        <div>
          <h3>{item.productName}</h3>
          <span>{item.brand || 'Sin marca'}</span>
        </div>
        <strong data-available={item.available === true ? 'true' : String(item.available)}>
          {item.available == null
            ? 'Disponibilidad desconocida'
            : item.available
              ? 'Disponible'
              : 'No disponible'}
        </strong>
      </header>
      <dl>
        <Definition
          label="Cantidad requerida"
          value={formatQuantity(item.requiredQuantity, item.requiredUnit)}
        />
        <Definition
          label="Formato"
          value={
            item.packageQuantity == null
              ? 'No calculable'
              : formatQuantity(item.packageQuantity, item.packageUnit ?? '')
          }
        />
        <Definition
          label="Paquetes"
          value={item.packagesRequired == null ? 'No calculable' : String(item.packagesRequired)}
        />
        <Definition
          label="Cantidad comprada"
          value={
            item.purchasedQuantity == null
              ? 'No calculable'
              : formatQuantity(item.purchasedQuantity, item.requiredUnit)
          }
        />
        <Definition
          label="Sobrante"
          value={
            item.leftoverQuantity == null
              ? 'No calculable'
              : `${formatQuantity(item.leftoverQuantity, item.requiredUnit)} (${formatDecimal(
                  item.leftoverPercentage ?? 0,
                )} %)`
          }
        />
        <Definition label="Coste consumido" value={nullableMoney(item.consumedCost)} />
        <Definition label="Coste real de compra" value={nullableMoney(item.purchaseCost)} />
        <Definition label="Coste sobrante" value={nullableMoney(item.wasteCost)} />
      </dl>
      {item.warnings.length > 0 && (
        <ul className={styles.itemWarnings}>
          {item.warnings.map((warning) => (
            <li key={warning}>⚠ {warning}</li>
          ))}
        </ul>
      )}
    </article>
  )
}

function Definition({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function nullableMoney(value: number | null): string {
  return value == null ? 'No calculable' : formatMoney(value)
}

function formatQuantity(value: number, unit: string): string {
  return `${formatDecimal(value)} ${unit}`
}

function measurementLabel(value: string): string {
  return { WEIGHT: 'Peso', VOLUME: 'Volumen', UNIT: 'Unidades' }[value] ?? value
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.main}>{children}</main>
    </div>
  )
}

function State({ text, error = false }: { text: string; error?: boolean }) {
  return (
    <div
      className={`${styles.state} ${error ? styles.error : ''}`}
      role={error ? 'alert' : 'status'}
    >
      {text}
    </div>
  )
}
