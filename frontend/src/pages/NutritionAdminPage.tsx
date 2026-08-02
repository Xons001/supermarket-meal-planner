import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  acceptNutritionCandidate,
  getNutritionCandidates,
  getNutritionOverview,
  getNutritionRuns,
  rejectNutritionCandidate,
  triggerNutritionEnrichment,
} from '../api/nutritionAdmin'
import { queryKeys } from '../app/queryKeys'
import { SiteHeader } from '../components/SiteHeader'
import { Badge, Button, Card, EmptyState, Pagination, Skeleton, useToast } from '../components/ui'
import { formatDate } from '../utils/format'
import styles from './NutritionAdminPage.module.css'

const running = (status?: string) => status === 'PENDING' || status === 'RUNNING'

export function NutritionAdminPage() {
  const [runPage, setRunPage] = useState(0)
  const [candidatePage, setCandidatePage] = useState(0)
  const client = useQueryClient()
  const notify = useToast()
  const overview = useQuery({
    queryKey: queryKeys.nutritionOverview,
    queryFn: getNutritionOverview,
    refetchInterval: 10_000,
  })
  const runs = useQuery({
    queryKey: queryKeys.nutritionRuns(runPage),
    queryFn: () => getNutritionRuns(runPage),
    placeholderData: keepPreviousData,
    refetchInterval: (query) =>
      query.state.data?.content.some((run) => running(run.status)) ? 2_000 : false,
  })
  const candidates = useQuery({
    queryKey: queryKeys.nutritionCandidates(candidatePage),
    queryFn: () => getNutritionCandidates(candidatePage),
    placeholderData: keepPreviousData,
  })
  const refresh = () => client.invalidateQueries({ queryKey: ['admin', 'nutrition'] })
  const trigger = useMutation({
    mutationFn: () => triggerNutritionEnrichment('LOCAL_JSON'),
    onSuccess: async () => {
      notify('Enriquecimiento aceptado por Airflow')
      await refresh()
    },
    onError: (error: Error) => notify(error.message, 'error'),
  })
  const accept = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      acceptNutritionCandidate(id, version),
    onSuccess: async () => {
      notify('Candidato aceptado')
      await refresh()
    },
    onError: (error: Error) => notify(error.message, 'error'),
  })
  const reject = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      rejectNutritionCandidate(id, version, 'No corresponde al producto revisado'),
    onSuccess: async () => {
      notify('Candidato rechazado')
      await refresh()
    },
    onError: (error: Error) => notify(error.message, 'error'),
  })
  return (
    <>
      <SiteHeader />
      <main className={styles.page}>
        <header className={styles.hero}>
          <div>
            <p>Administración · FASE 10</p>
            <h1>Calidad nutricional</h1>
            <span>
              Origen, confianza, revisión e historial sin sobrescribir correcciones manuales.
            </span>
          </div>
          <Button
            disabled={
              trigger.isPending ||
              !overview.data?.enabled ||
              running(overview.data?.latestRun?.status)
            }
            onClick={() => trigger.mutate()}
          >
            Enriquecer nutrición
          </Button>
        </header>
        {overview.isPending ? (
          <Skeleton height="8rem" />
        ) : overview.isError ? (
          <EmptyState title="No se pudo cargar el resumen" />
        ) : (
          <section className={styles.overview}>
            <Card>
              <span>Sin datos</span>
              <strong>{overview.data.productsWithoutNutrition}</strong>
            </Card>
            <Card>
              <span>Datos parciales</span>
              <strong>{overview.data.partialProducts}</strong>
            </Card>
            <Card>
              <span>Verificados</span>
              <strong>{overview.data.verifiedProducts}</strong>
            </Card>
            <Card>
              <span>Por revisar</span>
              <strong>{overview.data.pendingCandidates}</strong>
            </Card>
          </section>
        )}
        <section>
          <h2>Candidatos pendientes</h2>
          {candidates.isPending ? (
            <Skeleton height="8rem" />
          ) : candidates.isError ? (
            <EmptyState title="No se pudieron cargar los candidatos" />
          ) : candidates.data.content.length === 0 ? (
            <EmptyState title="No hay revisiones pendientes">
              <p>Las coincidencias ambiguas aparecerán aquí.</p>
            </EmptyState>
          ) : (
            <div className={styles.cards}>
              {candidates.data.content.map((candidate) => (
                <Card key={candidate.id}>
                  <div className={styles.candidateTitle}>
                    <div>
                      <strong>{candidate.productName}</strong>
                      <span>{candidate.externalName}</span>
                    </div>
                    <Badge tone="warning">{candidate.confidenceScore}%</Badge>
                  </div>
                  <p>
                    {candidate.matchMethod} · {candidate.provider} · caduca{' '}
                    {formatDate(candidate.expiresAt)}
                  </p>
                  <div className={styles.actions}>
                    <Button disabled={accept.isPending} onClick={() => accept.mutate(candidate)}>
                      Aceptar
                    </Button>
                    <Button
                      variant="secondary"
                      disabled={reject.isPending}
                      onClick={() => reject.mutate(candidate)}
                    >
                      Rechazar
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          )}
          {candidates.data && (
            <Pagination
              page={candidates.data.page}
              totalPages={candidates.data.totalPages}
              onPage={setCandidatePage}
            />
          )}
        </section>
        <section>
          <h2>Últimas ejecuciones</h2>
          {runs.isPending ? (
            <Skeleton height="8rem" />
          ) : runs.isError ? (
            <EmptyState title="No se pudo cargar el historial" />
          ) : runs.data.content.length === 0 ? (
            <EmptyState title="Todavía no hay ejecuciones" />
          ) : (
            <div className={styles.tableWrap}>
              <table>
                <thead>
                  <tr>
                    <th>Estado</th>
                    <th>Proveedor</th>
                    <th>Escaneados</th>
                    <th>Actualizados</th>
                    <th>Revisión</th>
                    <th>Fecha</th>
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
                              : run.status === 'FAILED'
                                ? 'warning'
                                : 'neutral'
                          }
                        >
                          {run.status}
                        </Badge>
                      </td>
                      <td>{run.provider}</td>
                      <td>{run.productsScanned}</td>
                      <td>{run.updatedProducts}</td>
                      <td>{run.pendingReview}</td>
                      <td>{formatDate(run.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {runs.data && (
            <Pagination
              page={runs.data.page}
              totalPages={runs.data.totalPages}
              onPage={setRunPage}
            />
          )}
        </section>
      </main>
    </>
  )
}
