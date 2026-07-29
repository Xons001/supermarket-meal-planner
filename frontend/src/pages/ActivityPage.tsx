import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useSearchParams, Link } from 'react-router'
import { getActivity } from '../api/dashboard'
import { queryKeys } from '../app/queryKeys'
import { SiteHeader } from '../components/SiteHeader'
import { Badge, EmptyState, FilterBar, Pagination, Skeleton } from '../components/ui'
import { formatDate } from '../utils/format'
import styles from './ActivityPage.module.css'

export function ActivityPage() {
  const [params, setParams] = useSearchParams()
  const type = params.get('type') ?? ''
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)
  const activity = useQuery({
    queryKey: queryKeys.activity(type, page),
    queryFn: () => getActivity(type, page),
    placeholderData: keepPreviousData,
  })
  return (
    <>
      <SiteHeader />
      <main className={styles.page}>
        <header>
          <p>Historial</p>
          <h1>Actividad</h1>
          <span>Un registro legible de tus planes, listas y ediciones.</span>
        </header>
        <FilterBar>
          <label>
            Tipo
            <select
              value={type}
              onChange={(event) => setParams({ type: event.target.value, page: '0' })}
            >
              <option value="">Toda la actividad</option>
              <option value="MEAL_PLAN_CREATED">Planes creados</option>
              <option value="MEAL_PLAN_DUPLICATED">Planes duplicados</option>
              <option value="SHOPPING_LIST_CREATED">Listas creadas</option>
              <option value="MEAL_REGENERATED">Comidas regeneradas</option>
            </select>
          </label>
        </FilterBar>
        {activity.isPending ? (
          <div className={styles.loading}>
            <Skeleton height="5rem" />
            <Skeleton height="5rem" />
          </div>
        ) : activity.isError ? (
          <EmptyState title="No se pudo cargar la actividad" />
        ) : activity.data.content.length === 0 ? (
          <EmptyState title="Todavía no hay actividad">
            <p>Las acciones nuevas aparecerán aquí sin mostrar JSON técnico.</p>
          </EmptyState>
        ) : (
          <>
            <ol className={styles.feed}>
              {activity.data.content.map((item) => (
                <li key={item.id}>
                  <div>
                    <Badge tone={item.origin === 'BACKFILLED' ? 'neutral' : 'success'}>
                      {item.origin === 'BACKFILLED' ? 'Histórico' : 'Nuevo'}
                    </Badge>
                    <Link to={item.link}>{item.summary}</Link>
                  </div>
                  <time dateTime={item.occurredAt}>{formatDate(item.occurredAt)}</time>
                </li>
              ))}
            </ol>
            <Pagination
              page={activity.data.page}
              totalPages={activity.data.totalPages}
              onPage={(next) => setParams({ type, page: String(next) })}
            />
          </>
        )}
      </main>
    </>
  )
}
