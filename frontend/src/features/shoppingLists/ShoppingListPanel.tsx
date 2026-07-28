import { Link } from 'react-router-dom'
import { ApiError } from '../../api/client'
import {
  useArchiveShoppingList,
  useCreateShoppingList,
  useRegenerateShoppingList,
  useShoppingListForPlan,
} from '../../hooks/useShoppingListQueries'
import { formatDecimal, formatMoney } from '../../utils/format'
import styles from './ShoppingListPanel.module.css'

export function ShoppingListPanel({ mealPlanId }: { mealPlanId: string }) {
  const list = useShoppingListForPlan(mealPlanId)
  const create = useCreateShoppingList(mealPlanId)
  const regenerate = useRegenerateShoppingList(mealPlanId)
  const archive = useArchiveShoppingList(mealPlanId)
  const notFound = list.error instanceof ApiError && list.error.status === 404

  async function regenerateList() {
    if (!window.confirm('¿Regenerar la lista? Se conservará la anterior si el cálculo falla.'))
      return
    await regenerate.mutateAsync()
  }

  async function archiveList() {
    if (!window.confirm('¿Archivar esta lista de compra?')) return
    await archive.mutateAsync()
  }

  return (
    <section className={styles.panel} aria-labelledby="shopping-list-title">
      <header>
        <div>
          <span>FASE 4</span>
          <h2 id="shopping-list-title">Lista de compra</h2>
          <p>Paquetes completos, coste real estimado y sobrantes del plan.</p>
        </div>
        <strong>Datos de demostración · El precio puede variar</strong>
      </header>

      {list.isPending ? (
        <p role="status">Comprobando lista de compra…</p>
      ) : notFound ? (
        <div className={styles.empty}>
          <p>No se ha generado una lista para este plan.</p>
          <button onClick={() => create.mutate()} disabled={create.isPending}>
            {create.isPending ? 'Generando…' : 'Generar lista de compra'}
          </button>
        </div>
      ) : list.isError ? (
        <p role="alert" className={styles.error}>
          No se ha podido consultar la lista de compra.
        </p>
      ) : (
        <>
          <div className={styles.metrics}>
            <Metric label="Coste consumido" value={formatMoney(list.data.totalConsumedCost)} />
            <Metric label="Coste real de compra" value={formatMoney(list.data.totalPurchaseCost)} />
            <Metric
              label="Coste estimado de sobrantes"
              value={formatMoney(list.data.totalWasteCost)}
            />
            <Metric label="Productos" value={String(list.data.itemCount)} />
            <Metric label="Paquetes" value={String(list.data.totalPackages)} />
            <Metric
              label="Desperdicio"
              value={`${formatDecimal(list.data.overallWastePercentage)} %`}
            />
            <Metric
              label="Presupuesto"
              value={
                list.data.weeklyBudget == null
                  ? 'Sin presupuesto'
                  : formatMoney(list.data.weeklyBudget)
              }
            />
            <Metric
              label="Diferencia"
              value={
                list.data.purchaseBudgetDifference == null
                  ? 'No aplicable'
                  : formatMoney(list.data.purchaseBudgetDifference)
              }
            />
          </div>
          {!list.data.calculationComplete && (
            <p className={styles.partial}>
              Cálculo parcial: uno o más productos no disponen de información suficiente.
            </p>
          )}
          {list.data.weeklyBudget != null && (
            <p className={styles.budget}>
              {list.data.purchaseBudgetExceeded
                ? 'Presupuesto real superado.'
                : 'Presupuesto real no superado.'}{' '}
              {!list.data.budgetCalculationComplete && 'Comparación parcial.'}
            </p>
          )}
          <div className={styles.actions}>
            <Link to={`/shopping-lists/${list.data.id}`}>Ver lista de compra</Link>
            <button onClick={() => void regenerateList()} disabled={regenerate.isPending}>
              {regenerate.isPending ? 'Regenerando…' : 'Regenerar'}
            </button>
            <button onClick={() => void archiveList()} disabled={archive.isPending}>
              Archivar lista
            </button>
          </div>
        </>
      )}
      {(create.isError || regenerate.isError || archive.isError) && (
        <p role="alert" className={styles.error}>
          {create.error?.message ?? regenerate.error?.message ?? archive.error?.message}
        </p>
      )}
    </section>
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
