import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import {
  confirmEdit,
  getMealAlternatives,
  previewMealReplacement,
  previewRegeneration,
  setMealLock,
  undoMealPlanEdit,
} from '../../api/mealPlans'
import { ApiError } from '../../api/client'
import { useMealPlanChanges, useRefreshEditedPlan } from '../../hooks/useMealPlanQueries'
import type {
  AlternativePriority,
  EditPreview,
  GeneratedMealPlan,
  PlannedMeal,
} from '../../types/mealPlan'
import { formatDecimal, formatMoney } from '../../utils/format'
import styles from './MealPlanEditingPanel.module.css'

type Target = { kind: 'meal' | 'day'; id: string; name: string; meal?: PlannedMeal }

export function MealPlanEditingPanel({ plan }: { plan: GeneratedMealPlan }) {
  const planId = plan.mealPlanId ?? ''
  const editVersion = plan.editVersion ?? 0
  const refresh = useRefreshEditedPlan(planId)
  const history = useMealPlanChanges(planId)
  const [target, setTarget] = useState<Target | null>(null)
  const [priority, setPriority] = useState<AlternativePriority>('BEST_BALANCE')
  const [seed, setSeed] = useState('')
  const [preview, setPreview] = useState<EditPreview | null>(null)
  const [error, setError] = useState('')

  const alternatives = useQuery({
    queryKey: ['meal-alternatives', planId, target?.id, priority, seed],
    queryFn: () =>
      getMealAlternatives(
        planId,
        target?.id ?? '',
        priority,
        seed === '' ? undefined : Number(seed),
      ),
    enabled: target?.kind === 'meal' && !target.meal?.locked,
  })

  const mutation = useMutation({
    mutationFn: async (work: () => Promise<GeneratedMealPlan | EditPreview>) => work(),
    onError: (value) => setError(problemMessage(value)),
  })

  async function runPreview(work: () => Promise<EditPreview>) {
    setError('')
    const value = await mutation.mutateAsync(work)
    setPreview(value as EditPreview)
  }

  async function applyPreview() {
    if (!preview || !target) return
    setError('')
    const isDay = target.kind === 'day'
    const result = (await mutation.mutateAsync(() =>
      confirmEdit(
        planId,
        isDay ? 'days' : 'meals',
        target.id,
        preview.operation === 'MEAL_REPLACED' ? 'replacements' : 'regenerations',
        preview.previewToken,
        editVersion,
      ),
    )) as GeneratedMealPlan
    setPreview(null)
    setTarget(null)
    refresh(result)
  }

  async function toggleLock(meal: PlannedMeal) {
    if (!meal.plannedMealId) return
    const result = (await mutation.mutateAsync(() =>
      setMealLock(planId, meal.plannedMealId!, !meal.locked, editVersion),
    )) as GeneratedMealPlan
    refresh(result)
  }

  async function undo() {
    const result = (await mutation.mutateAsync(() =>
      undoMealPlanEdit(planId, editVersion),
    )) as GeneratedMealPlan
    refresh(result)
  }

  if (!plan.persisted || !planId) return null

  return (
    <section className={styles.panel} aria-labelledby="editing-title">
      <header>
        <div>
          <span>Edición parcial · versión {editVersion}</span>
          <h2 id="editing-title">Ajusta el plan sin regenerar toda la semana</h2>
        </div>
        <button disabled={!plan.canUndo || mutation.isPending} onClick={() => void undo()}>
          Deshacer último cambio
        </button>
      </header>

      {plan.status === 'ARCHIVED' && (
        <p role="status">Este plan está archivado. Reactívalo antes de editarlo.</p>
      )}
      {plan.shoppingListStatus === 'OUTDATED' && (
        <p className={styles.warning} role="status">
          La lista activa corresponde a una versión anterior. Sigue accesible, pero debes generar
          otra explícitamente para actualizarla.
        </p>
      )}
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      <div className={styles.days}>
        {plan.days.map((day) => (
          <article key={day.dayId ?? day.date}>
            <div className={styles.dayHeading}>
              <strong>Día {day.dayIndex + 1}</strong>
              <button
                disabled={!day.dayId || mutation.isPending || plan.status === 'ARCHIVED'}
                onClick={() => {
                  const next = {
                    kind: 'day' as const,
                    id: day.dayId!,
                    name: `Día ${day.dayIndex + 1}`,
                  }
                  setTarget(next)
                  void runPreview(() =>
                    previewRegeneration(
                      planId,
                      'days',
                      next.id,
                      editVersion,
                      seed === '' ? undefined : Number(seed),
                    ),
                  )
                }}
              >
                Regenerar día
              </button>
            </div>
            {day.meals.map((meal) => (
              <div className={styles.meal} key={meal.plannedMealId ?? meal.position}>
                <div>
                  <strong>{meal.templateName}</strong>
                  <small>
                    {meal.locked ? 'Bloqueada' : 'Editable'} · {meal.selectionSource ?? 'GENERATED'}
                  </small>
                </div>
                <div className={styles.actions}>
                  <button
                    disabled={!meal.plannedMealId || mutation.isPending}
                    aria-label={`${meal.locked ? 'Desbloquear' : 'Bloquear'} ${meal.templateName}`}
                    onClick={() => void toggleLock(meal)}
                  >
                    {meal.locked ? 'Desbloquear' : 'Bloquear'}
                  </button>
                  <button
                    disabled={meal.locked || !meal.plannedMealId || plan.status === 'ARCHIVED'}
                    onClick={() => {
                      setPreview(null)
                      setTarget({
                        kind: 'meal',
                        id: meal.plannedMealId!,
                        name: meal.templateName,
                        meal,
                      })
                    }}
                  >
                    Cambiar
                  </button>
                  <button
                    disabled={meal.locked || !meal.plannedMealId || mutation.isPending}
                    onClick={() => {
                      const next = {
                        kind: 'meal' as const,
                        id: meal.plannedMealId!,
                        name: meal.templateName,
                        meal,
                      }
                      setTarget(next)
                      void runPreview(() =>
                        previewRegeneration(
                          planId,
                          'meals',
                          next.id,
                          editVersion,
                          seed === '' ? undefined : Number(seed),
                        ),
                      )
                    }}
                  >
                    Regenerar
                  </button>
                </div>
              </div>
            ))}
          </article>
        ))}
      </div>

      {target?.kind === 'meal' && !preview && (
        <div className={styles.workspace} aria-live="polite">
          <div className={styles.controls}>
            <label>
              Prioridad
              <select
                value={priority}
                onChange={(event) => setPriority(event.target.value as AlternativePriority)}
              >
                <option value="BEST_BALANCE">Mejor equilibrio</option>
                <option value="LOWER_PURCHASE_COST">Menor compra</option>
                <option value="LOWER_WASTE">Menor desperdicio</option>
                <option value="MORE_VARIETY">Más variedad</option>
              </select>
            </label>
            <Seed value={seed} onChange={setSeed} />
          </div>
          {alternatives.isPending && <p role="status">Buscando alternativas…</p>}
          {alternatives.isError && <p role="alert">{problemMessage(alternatives.error)}</p>}
          {alternatives.data?.length === 0 && <p>No hay alternativas válidas.</p>}
          <div className={styles.alternatives}>
            {alternatives.data?.map((item) => (
              <article key={item.mealTemplateId}>
                <strong>{item.name}</strong>
                <span>
                  {formatDecimal(item.calories)} kcal · {formatMoney(item.purchaseCostDelta)} compra
                </span>
                <p>{item.reasons.join(' · ')}</p>
                <button
                  disabled={mutation.isPending}
                  onClick={() =>
                    void runPreview(() =>
                      previewMealReplacement(
                        planId,
                        target.id,
                        item.mealTemplateId,
                        editVersion,
                        item.seed,
                      ),
                    )
                  }
                >
                  Ver antes y después
                </button>
              </article>
            ))}
          </div>
        </div>
      )}

      {preview && (
        <Preview
          value={preview}
          pending={mutation.isPending}
          onConfirm={() => void applyPreview()}
          onCancel={() => setPreview(null)}
        />
      )}

      <details className={styles.history}>
        <summary>Historial de cambios</summary>
        {history.isPending && <p>Cargando historial…</p>}
        <ol>
          {history.data?.content?.map((change) => (
            <li key={change.id}>
              <strong>{changeLabel(change.type)}</strong>
              <span>
                versión {change.editVersion} · {new Date(change.createdAt).toLocaleString('es-ES')}
                {change.undone ? ' · deshecho' : ''}
              </span>
            </li>
          ))}
        </ol>
      </details>
    </section>
  )
}

function Preview({
  value,
  pending,
  onConfirm,
  onCancel,
}: {
  value: EditPreview
  pending: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  const metrics: Array<[string, keyof EditPreview['before'], 'money' | 'number']> = [
    ['Calorías', 'calories', 'number'],
    ['Proteína', 'protein', 'number'],
    ['Coste consumido', 'consumedCost', 'money'],
    ['Compra', 'purchaseCost', 'money'],
    ['Desperdicio', 'wasteCost', 'money'],
    ['Envases', 'packages', 'number'],
    ['Productos', 'uniqueProducts', 'number'],
    ['Variedad', 'varietyScore', 'number'],
    ['Repetición', 'repetitionScore', 'number'],
    ['Puntuación', 'overallScore', 'number'],
    ['Margen presupuestario', 'budgetDifference', 'money'],
  ]
  return (
    <div className={styles.preview} role="dialog" aria-modal="true" aria-labelledby="preview-title">
      <h3 id="preview-title">Confirma el cambio</h3>
      <div className={styles.metricHeader}>
        <span>Métrica</span>
        <span>Antes</span>
        <span>Después</span>
        <span>Diferencia</span>
      </div>
      {metrics.map(([label, key, kind]) => {
        const before = value.before[key] as number | null
        const after = value.after[key] as number | null
        const delta = value.delta[key] as number | null
        return (
          <div className={styles.metricRow} key={key}>
            <strong>{label}</strong>
            <span>{metricValue(before, kind)}</span>
            <span>{metricValue(after, kind)}</span>
            <span>
              {metricValue(delta, kind)} · {deltaMeaning(key, delta)}
            </span>
          </div>
        )
      })}
      {value.warnings.length > 0 && <p className={styles.warning}>{value.warnings.join(' · ')}</p>}
      <p>{value.reasons.join(' · ')}</p>
      <div className={styles.actions}>
        <button onClick={onCancel}>Cancelar</button>
        <button disabled={pending} onClick={onConfirm}>
          Confirmar cambio
        </button>
      </div>
    </div>
  )
}

function Seed({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <label>
      Seed opcional
      <input type="number" value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  )
}

function metricValue(value: number | null, kind: 'money' | 'number') {
  if (value === null || value === undefined) return 'No disponible'
  return kind === 'money' ? formatMoney(value) : formatDecimal(value)
}

function deltaMeaning(key: keyof EditPreview['before'], delta: number | null) {
  if (delta === null || delta === 0) return 'sin cambio'
  const lowerIsBetter = ['purchaseCost', 'wasteCost', 'packages', 'uniqueProducts'].includes(key)
  return lowerIsBetter ? (delta < 0 ? 'mejora' : 'empeora') : delta > 0 ? 'mejora' : 'empeora'
}

function problemMessage(value: unknown) {
  if (!(value instanceof ApiError)) return 'No se ha podido completar la operación.'
  const code = value.problem?.errorCode
  if (value.status === 409)
    return `El plan cambió mientras editabas. Recarga e inténtalo de nuevo${code ? ` (${code})` : ''}.`
  if (value.status === 422)
    return `La operación no cumple las reglas del plan${code ? ` (${code})` : ''}.`
  if (value.status === 400) return `El preview ya no es válido${code ? ` (${code})` : ''}.`
  return value.message
}

function changeLabel(type: string) {
  return (
    (
      {
        MEAL_REPLACED: 'Comida sustituida',
        MEAL_REGENERATED: 'Comida regenerada',
        DAY_REGENERATED: 'Día regenerado',
        MEAL_LOCKED: 'Comida bloqueada',
        MEAL_UNLOCKED: 'Comida desbloqueada',
        CHANGE_UNDONE: 'Cambio deshecho',
      } as Record<string, string>
    )[type] ?? type
  )
}
