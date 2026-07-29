import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { getDashboard } from '../api/dashboard'
import { queryKeys } from '../app/queryKeys'
import { SiteHeader } from '../components/SiteHeader'
import { Badge, Card, EmptyState, Skeleton } from '../components/ui'
import { formatDate, formatMoney } from '../utils/format'
import styles from './DashboardPage.module.css'

export function DashboardPage() {
  const dashboard = useQuery({ queryKey: queryKeys.dashboard, queryFn: getDashboard })
  return (
    <>
      <SiteHeader />
      <main className={styles.page}>
        <header className={styles.hero}>
          <div>
            <p>Tu espacio</p>
            <h1>Dashboard</h1>
            <span>Planes, compra y actividad reciente en un solo lugar.</span>
          </div>
          <Link to="/meal-plans/new">Crear un plan</Link>
        </header>
        {dashboard.isPending ? (
          <div className={styles.skeletons} aria-label="Cargando dashboard">
            <Skeleton height="7rem" />
            <Skeleton height="14rem" />
          </div>
        ) : dashboard.isError ? (
          <EmptyState title="No pudimos cargar tu dashboard">
            <p>Comprueba la conexión e inténtalo de nuevo.</p>
            <button onClick={() => void dashboard.refetch()}>Reintentar</button>
          </EmptyState>
        ) : (
          <>
            <section className={styles.metrics} aria-label="Resumen">
              <Metric label="Planes activos" value={dashboard.data.metrics.activePlans} />
              <Metric label="Favoritos" value={dashboard.data.metrics.favoritePlans} />
              <Metric label="Listas actuales" value={dashboard.data.metrics.currentShoppingLists} />
              <Metric
                label="Compra media"
                value={
                  dashboard.data.metrics.averagePurchaseCost === null
                    ? '—'
                    : formatMoney(dashboard.data.metrics.averagePurchaseCost)
                }
              />
            </section>
            <section className={styles.columns}>
              <Card>
                <div className={styles.cardHeading}>
                  <h2>Último plan activo</h2>
                  <Link to="/meal-plans">Ver todos</Link>
                </div>
                {dashboard.data.latestPlan ? (
                  <div className={styles.plan}>
                    <Badge tone={dashboard.data.latestPlan.favorite ? 'success' : 'neutral'}>
                      {dashboard.data.latestPlan.favorite ? 'Favorito' : 'Activo'}
                    </Badge>
                    <h3>{dashboard.data.latestPlan.name}</h3>
                    <p>Desde el {formatDate(dashboard.data.latestPlan.startDate)}</p>
                    <dl>
                      <Metric
                        label="Compra"
                        value={
                          dashboard.data.latestPlan.estimatedPurchaseCost === null
                            ? 'Cálculo parcial'
                            : formatMoney(dashboard.data.latestPlan.estimatedPurchaseCost)
                        }
                      />
                      <Metric
                        label="Desperdicio"
                        value={
                          dashboard.data.latestPlan.estimatedWasteCost === null
                            ? '—'
                            : formatMoney(dashboard.data.latestPlan.estimatedWasteCost)
                        }
                      />
                    </dl>
                    <Link to={`/meal-plans/${dashboard.data.latestPlan.id}`}>Abrir plan</Link>
                  </div>
                ) : (
                  <EmptyState title="Aún no tienes planes">
                    <Link to="/meal-plans/new">Generar el primero</Link>
                  </EmptyState>
                )}
              </Card>
              <Card>
                <div className={styles.cardHeading}>
                  <h2>Lista seleccionada</h2>
                  <Link to="/shopping-lists">Historial</Link>
                </div>
                {dashboard.data.selectedShoppingList ? (
                  <div className={styles.plan}>
                    <Badge
                      tone={
                        dashboard.data.selectedShoppingList.freshness === 'CURRENT'
                          ? 'success'
                          : 'warning'
                      }
                    >
                      {dashboard.data.selectedShoppingList.freshness === 'CURRENT'
                        ? 'Actual'
                        : 'Desactualizada'}
                    </Badge>
                    <h3>{dashboard.data.selectedShoppingList.mealPlanName}</h3>
                    <p>
                      Compra {formatMoney(dashboard.data.selectedShoppingList.totalPurchaseCost)} ·
                      sobrante {formatMoney(dashboard.data.selectedShoppingList.totalWasteCost)}
                    </p>
                    <Link to={`/shopping-lists/${dashboard.data.selectedShoppingList.id}`}>
                      Abrir lista
                    </Link>
                  </div>
                ) : (
                  <EmptyState title="Sin lista seleccionada">
                    <p>Genera una desde el detalle de un plan.</p>
                  </EmptyState>
                )}
              </Card>
            </section>
            <Card>
              <div className={styles.cardHeading}>
                <h2>Actividad reciente</h2>
                <Link to="/activity">Ver actividad</Link>
              </div>
              {dashboard.data.recentActivity.length ? (
                <ul className={styles.activity}>
                  {dashboard.data.recentActivity.map((item) => (
                    <li key={item.id}>
                      <Link to={item.link}>{item.summary}</Link>
                      <time dateTime={item.occurredAt}>{formatDate(item.occurredAt)}</time>
                    </li>
                  ))}
                </ul>
              ) : (
                <p>Tu actividad aparecerá aquí.</p>
              )}
            </Card>
          </>
        )}
      </main>
    </>
  )
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}
