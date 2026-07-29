import { useNavigate, useParams } from 'react-router'
import { SiteHeader } from '../components/SiteHeader'
import { MealPlanResult } from '../features/mealPlans/MealPlanResult'
import { MealPlanEditingPanel } from '../features/mealPlans/MealPlanEditingPanel'
import { ShoppingListPanel } from '../features/shoppingLists/ShoppingListPanel'
import {
  useArchiveMealPlan,
  useChangeMealPlanStatus,
  useMealPlan,
} from '../hooks/useMealPlanQueries'
import styles from './MealPlanDetailPage.module.css'

export function MealPlanDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const plan = useMealPlan(id)
  const status = useChangeMealPlanStatus(id)
  const archive = useArchiveMealPlan(id)

  async function archivePlan() {
    if (!window.confirm('¿Archivar este plan? Seguirá disponible en el historial.')) return
    await archive.mutateAsync()
    await plan.refetch()
  }

  if (plan.isPending)
    return (
      <Shell>
        <State text="Cargando plan…" />
      </Shell>
    )
  if (plan.isError)
    return (
      <Shell>
        <State text="No se ha encontrado el plan solicitado." error />
      </Shell>
    )

  return (
    <Shell>
      <header className={styles.heading}>
        <div>
          <span>{plan.data.status} · snapshot guardado</span>
          <h1>{plan.data.name}</h1>
          <p>
            {plan.data.supermarketName} · seed {plan.data.seed} · {plan.data.numberOfDays} días
          </p>
        </div>
        <div>
          {plan.data.status === 'ARCHIVED' ? (
            <button onClick={() => status.mutate('GENERATED')} disabled={status.isPending}>
              Reactivar
            </button>
          ) : (
            <button onClick={() => void archivePlan()} disabled={archive.isPending}>
              Archivar
            </button>
          )}
          <button onClick={() => navigate('/meal-plans/new', { state: { sourcePlan: plan.data } })}>
            Crear otro con estos criterios
          </button>
          <button onClick={() => navigate('/meal-plans')}>Volver</button>
        </div>
      </header>
      <MealPlanEditingPanel plan={plan.data} />
      <ShoppingListPanel mealPlanId={id} />
      <MealPlanResult plan={plan.data} />
    </Shell>
  )
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
