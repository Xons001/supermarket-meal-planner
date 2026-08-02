import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router'
import {
  getCatalogSyncErrors,
  getCatalogSyncOverview,
  getCatalogSyncRuns,
  retryCatalogSync,
  triggerCatalogSync,
} from '../api/catalogSync'
import { queryKeys } from '../app/queryKeys'
import { SiteHeader } from '../components/SiteHeader'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  FilterBar,
  Modal,
  Pagination,
  Skeleton,
  useToast,
} from '../components/ui'
import type { CatalogSyncRun, CatalogSyncStatus } from '../types/catalogSync'
import { formatDate } from '../utils/format'
import styles from './CatalogSyncAdminPage.module.css'

const active = (status: CatalogSyncStatus) => status === 'PENDING' || status === 'RUNNING'
const statusLabel: Record<CatalogSyncStatus, string> = {
  PENDING: 'Pendiente',
  RUNNING: 'En curso',
  SUCCESS: 'Correcta',
  PARTIAL_SUCCESS: 'Parcial',
  FAILED: 'Fallida',
}

export function CatalogSyncAdminPage() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)
  const status = params.get('status') ?? ''
  const syncType = params.get('syncType') ?? ''
  const filters = { page, status, syncType }
  const [selected, setSelected] = useState<CatalogSyncRun | null>(null)
  const client = useQueryClient()
  const notify = useToast()
  const overview = useQuery({
    queryKey: queryKeys.catalogSyncOverview,
    queryFn: getCatalogSyncOverview,
    refetchInterval: 10_000,
  })
  const runs = useQuery({
    queryKey: queryKeys.catalogSyncRuns(filters),
    queryFn: () => getCatalogSyncRuns(filters),
    placeholderData: keepPreviousData,
    refetchInterval: (query) =>
      query.state.data?.content.some((run) => active(run.status)) ? 2_000 : false,
  })
  const errors = useQuery({
    queryKey: queryKeys.catalogSyncErrors(selected?.id ?? ''),
    queryFn: () => getCatalogSyncErrors(selected!.id),
    enabled: Boolean(selected && selected.validationErrors > 0),
  })
  const refresh = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ['admin', 'catalog-sync'] }),
      client.invalidateQueries({ queryKey: queryKeys.catalogSyncOverview }),
    ])
  }
  const trigger = useMutation({
    mutationFn: triggerCatalogSync,
    onSuccess: async () => {
      notify('Sincronización aceptada por Airflow')
      await refresh()
    },
    onError: (error: Error) => notify(error.message, 'error'),
  })
  const retry = useMutation({
    mutationFn: retryCatalogSync,
    onSuccess: async () => {
      notify('Reintento aceptado')
      setSelected(null)
      await refresh()
    },
    onError: (error: Error) => notify(error.message, 'error'),
  })
  const changeFilters = (next: { status?: string; syncType?: string; page?: number }) => {
    const values = { status, syncType, page: String(next.page ?? 0), ...next }
    setParams(
      Object.fromEntries(Object.entries(values).filter(([, value]) => value !== '')) as Record<
        string,
        string
      >,
    )
  }

  return (
    <>
      <SiteHeader />
      <main className={styles.page}>
        <header className={styles.hero}>
          <div>
            <p>Administración · FASE 9</p>
            <h1>Sincronización del catálogo</h1>
            <span>Catálogo y precios procesados por Airflow con staging e idempotencia.</span>
          </div>
          {overview.data?.airflowPublicUrl && (
            <a href={overview.data.airflowPublicUrl} target="_blank" rel="noreferrer">
              Abrir Airflow ↗
            </a>
          )}
        </header>

        {overview.isPending ? (
          <Skeleton height="8rem" />
        ) : overview.isError ? (
          <EmptyState title="No se pudo consultar Airflow" />
        ) : (
          <section className={styles.overview}>
            <Card>
              <span>Orquestador</span>
              <strong>{overview.data.airflowHealthy ? 'Conectado' : 'No disponible'}</strong>
              <Badge tone={overview.data.airflowHealthy ? 'success' : 'warning'}>
                {overview.data.airflowHealthy ? 'Saludable' : 'Revisar'}
              </Badge>
            </Card>
            <Card>
              <span>Proveedor</span>
              <strong>{overview.data.provider}</strong>
              <small>Mercadona experimental deshabilitado</small>
            </Card>
            <Card>
              <span>Catálogo completo</span>
              <strong>{overview.data.fullSchedule}</strong>
              <small>Europe/Madrid</small>
            </Card>
            <Card>
              <span>Solo precios</span>
              <strong>{overview.data.priceSchedule}</strong>
              <small>Europe/Madrid</small>
            </Card>
          </section>
        )}

        <section className={styles.actionsPanel} aria-labelledby="manual-title">
          <div>
            <h2 id="manual-title">Ejecución manual</h2>
            <p>Airflow responderá en segundo plano; esta pantalla actualizará el estado.</p>
          </div>
          <div>
            <Button
              disabled={trigger.isPending || !overview.data?.enabled}
              onClick={() => trigger.mutate('FULL_CATALOG')}
            >
              Sincronizar catálogo
            </Button>
            <Button
              variant="secondary"
              disabled={trigger.isPending || !overview.data?.enabled}
              onClick={() => trigger.mutate('PRICES_ONLY')}
            >
              Actualizar precios
            </Button>
          </div>
        </section>

        <FilterBar>
          <label>
            Estado
            <select value={status} onChange={(e) => changeFilters({ status: e.target.value })}>
              <option value="">Todos</option>
              {Object.entries(statusLabel).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Tipo
            <select value={syncType} onChange={(e) => changeFilters({ syncType: e.target.value })}>
              <option value="">Todos</option>
              <option value="FULL_CATALOG">Catálogo completo</option>
              <option value="PRICES_ONLY">Solo precios</option>
            </select>
          </label>
        </FilterBar>

        {runs.isPending ? (
          <div className={styles.loading}>
            <Skeleton height="5rem" />
            <Skeleton height="5rem" />
          </div>
        ) : runs.isError ? (
          <EmptyState title="No se pudo cargar el historial" />
        ) : runs.data.content.length === 0 ? (
          <EmptyState title="Todavía no hay sincronizaciones">
            <p>Inicia una ejecución manual o espera al horario programado.</p>
          </EmptyState>
        ) : (
          <>
            <div className={styles.tableWrap}>
              <table>
                <thead>
                  <tr>
                    <th>Estado</th>
                    <th>Tipo</th>
                    <th>Origen</th>
                    <th>Productos</th>
                    <th>Cambios</th>
                    <th>Solicitada</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {runs.data.content.map((run) => (
                    <tr key={run.id}>
                      <td>
                        <Badge
                          tone={
                            run.status === 'SUCCESS'
                              ? 'success'
                              : run.status === 'FAILED' || run.status === 'PARTIAL_SUCCESS'
                                ? 'warning'
                                : 'neutral'
                          }
                        >
                          {statusLabel[run.status]}
                        </Badge>
                      </td>
                      <td>{run.syncType === 'FULL_CATALOG' ? 'Catálogo' : 'Precios'}</td>
                      <td>{run.triggeredBy}</td>
                      <td>{run.productsProcessed}</td>
                      <td>{run.productsCreated + run.productsUpdated + run.pricesChanged}</td>
                      <td>
                        <time dateTime={run.requestedAt}>{formatDate(run.requestedAt)}</time>
                      </td>
                      <td>
                        <Button variant="secondary" onClick={() => setSelected(run)}>
                          Detalle
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={runs.data.page}
              totalPages={runs.data.totalPages}
              onPage={(next) => changeFilters({ page: next })}
            />
          </>
        )}
      </main>
      <Modal
        open={Boolean(selected)}
        title="Detalle de sincronización"
        onClose={() => setSelected(null)}
      >
        {selected && (
          <div className={styles.detail}>
            <Badge tone={selected.status === 'SUCCESS' ? 'success' : 'warning'}>
              {statusLabel[selected.status]}
            </Badge>
            <dl>
              <div>
                <dt>Procesados</dt>
                <dd>{selected.productsProcessed}</dd>
              </div>
              <div>
                <dt>Creados</dt>
                <dd>{selected.productsCreated}</dd>
              </div>
              <div>
                <dt>Actualizados</dt>
                <dd>{selected.productsUpdated}</dd>
              </div>
              <div>
                <dt>Precios</dt>
                <dd>{selected.pricesChanged}</dd>
              </div>
              <div>
                <dt>No disponibles</dt>
                <dd>{selected.productsUnavailable}</dd>
              </div>
              <div>
                <dt>Errores</dt>
                <dd>{selected.validationErrors}</dd>
              </div>
            </dl>
            {selected.validationErrors > 0 &&
              (errors.isPending ? (
                <Skeleton height="4rem" />
              ) : (
                <ul className={styles.errors}>
                  {errors.data?.content.map((error) => (
                    <li key={error.id}>
                      <strong>{error.errorCode}</strong>
                      <span>
                        {error.externalId ?? error.entityType}: {error.message}
                      </span>
                    </li>
                  ))}
                </ul>
              ))}
            {(selected.status === 'FAILED' || selected.status === 'PARTIAL_SUCCESS') && (
              <Button disabled={retry.isPending} onClick={() => retry.mutate(selected.id)}>
                Crear reintento
              </Button>
            )}
          </div>
        )}
      </Modal>
    </>
  )
}
