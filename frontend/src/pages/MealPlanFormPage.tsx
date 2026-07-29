import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { SiteHeader } from '../components/SiteHeader'
import { MealPlanResult } from '../features/mealPlans/MealPlanResult'
import {
  useAllergens,
  useDietaryTags,
  useProducts,
  useSupermarkets,
} from '../hooks/useCatalogQueries'
import { useGenerateMealPlan } from '../hooks/useMealPlanQueries'
import { useMealTemplates } from '../hooks/useMealTemplateQueries'
import { mealPlanFormSchema, type MealPlanFormValues } from '../schemas/mealPlanForm'
import type { GenerateMealPlanRequest, GeneratedMealPlan } from '../types/mealPlan'
import styles from './MealPlanFormPage.module.css'

const mealTypes = [
  ['BREAKFAST', 'Desayuno'],
  ['LUNCH', 'Comida'],
  ['SNACK', 'Merienda'],
  ['DINNER', 'Cena'],
] as const

export function MealPlanFormPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const sourcePlan = (location.state as { sourcePlan?: GeneratedMealPlan } | null)?.sourcePlan
  const generator = useGenerateMealPlan()
  const [preview, setPreview] = useState<GeneratedMealPlan>()
  const [lastRequest, setLastRequest] = useState<GenerateMealPlanRequest>()
  const supermarkets = useSupermarkets()
  const dietaryTags = useDietaryTags()
  const allergens = useAllergens()
  const templates = useMealTemplates({
    supermarketCode: 'MERCADONA',
    active: true,
    page: 0,
    size: 48,
    sort: 'name,asc',
  })
  const products = useProducts({
    supermarketCode: 'MERCADONA',
    available: true,
    page: 0,
    size: 48,
    sort: 'name,asc',
  })

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<MealPlanFormValues>({
    resolver: zodResolver(mealPlanFormSchema),
    defaultValues: sourcePlan
      ? valuesFromPlan(sourcePlan)
      : {
          supermarketCode: 'MERCADONA',
          name: 'Mi plan semanal',
          startDate: nextMonday(),
          numberOfDays: '7',
          mealsPerDay: '4',
          servings: '1',
          dailyCaloriesTarget: '2000',
          dailyProteinTarget: '100',
          weeklyBudget: '70',
          maximumPreparationMinutes: '40',
          maximumTemplateRepetitions: '3',
          varietyPreference: 'HIGH',
          allowedMealTypes: ['BREAKFAST', 'LUNCH', 'SNACK', 'DINNER'],
          requiredDietaryTags: [],
          excludedAllergens: [],
          excludedTemplateIds: [],
          excludedProductIds: [],
          allowIncompleteCalculations: false,
          strategy: 'PURCHASE_AWARE_SCORING',
          optimizationPreset: 'BALANCED',
          deterministicSeed: '',
        },
  })
  const selectedStrategy = useWatch({ control, name: 'strategy' })

  const loadingMetadata = supermarkets.isPending || dietaryTags.isPending || allergens.isPending
  const requestError = generator.error instanceof ApiError ? generator.error.message : undefined

  const templateOptions = useMemo(() => templates.data?.content ?? [], [templates.data?.content])

  async function previewPlan(values: MealPlanFormValues) {
    const request = toRequest(values, false)
    try {
      const result = await generator.mutateAsync(request)
      setLastRequest({ ...request, deterministicSeed: result.seed })
      setPreview(result)
    } catch {
      // React Query retains the Problem Details error for the inline explanation.
    }
  }

  async function savePreview() {
    if (!preview || !lastRequest) return
    try {
      const saved = await generator.mutateAsync({
        ...lastRequest,
        deterministicSeed: preview.seed,
        generationToken: preview.generationToken,
        persist: true,
      })
      if (saved.mealPlanId) navigate(`/meal-plans/${saved.mealPlanId}`)
    } catch {
      // The error remains visible without discarding the preview.
    }
  }

  async function regenerate(useSameSeed: boolean) {
    if (!lastRequest) return
    const request = {
      ...lastRequest,
      deterministicSeed: useSameSeed ? preview?.seed : undefined,
      generationToken: undefined,
      persist: false,
    }
    try {
      const result = await generator.mutateAsync(request)
      setLastRequest({ ...request, deterministicSeed: result.seed })
      setPreview(result)
    } catch {
      // The prior preview remains available if regeneration fails.
    }
  }

  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.main}>
        <header className={styles.intro}>
          <div>
            <span>FASE 3 · Generador determinista</span>
            <h1>Construye tu plan semanal</h1>
            <p>
              Define objetivos y restricciones. El motor compara plantillas con una estrategia
              scoring reproducible y explica el resultado.
            </p>
          </div>
          <Link to="/meal-plans">Ver planes guardados</Link>
        </header>

        {!preview && (
          <form className={styles.form} onSubmit={handleSubmit(previewPlan)}>
            <FormSection title="1. Datos básicos" description="Duración, raciones y supermercado.">
              <Field label="Supermercado" error={errors.supermarketCode?.message}>
                <select {...register('supermarketCode')} disabled={loadingMetadata}>
                  {supermarkets.data?.map((market) => (
                    <option key={market.code} value={market.code} disabled={!market.enabled}>
                      {market.name}
                      {!market.enabled ? ' (próximamente)' : ''}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Nombre del plan" error={errors.name?.message}>
                <input {...register('name')} />
              </Field>
              <Field label="Fecha de inicio" error={errors.startDate?.message}>
                <input type="date" {...register('startDate')} />
              </Field>
              <Field label="Número de días (1–14)" error={errors.numberOfDays?.message}>
                <input type="number" min="1" max="14" {...register('numberOfDays')} />
              </Field>
              <Field label="Comidas por día (1–6)" error={errors.mealsPerDay?.message}>
                <input type="number" min="1" max="6" {...register('mealsPerDay')} />
              </Field>
              <Field label="Raciones" error={errors.servings?.message}>
                <input type="number" min="1" {...register('servings')} />
              </Field>
            </FormSection>

            <FormSection
              title="2. Objetivos"
              description="Metas diarias y presupuesto orientativo."
            >
              <Field label="Calorías diarias" error={errors.dailyCaloriesTarget?.message}>
                <input type="number" min="1" {...register('dailyCaloriesTarget')} />
              </Field>
              <Field label="Proteína diaria (g)" error={errors.dailyProteinTarget?.message}>
                <input type="number" min="0" step="0.1" {...register('dailyProteinTarget')} />
              </Field>
              <Field label="Presupuesto semanal (€)" error={errors.weeklyBudget?.message}>
                <input type="number" min="0.01" step="0.01" {...register('weeklyBudget')} />
              </Field>
            </FormSection>

            <FormSection
              title="3. Optimización"
              description="Elige cómo se evalúa el coste del plan. Los presets no exponen pesos técnicos."
            >
              <Field label="Modo de generación" error={errors.strategy?.message}>
                <select {...register('strategy')}>
                  <option value="PURCHASE_AWARE_SCORING">Compra eficiente (recomendado)</option>
                  <option value="SCORING">Clásico: coste consumido</option>
                </select>
              </Field>
              {selectedStrategy === 'PURCHASE_AWARE_SCORING' && (
                <Field label="Prioridad" error={errors.optimizationPreset?.message}>
                  <select {...register('optimizationPreset')}>
                    <option value="BALANCED">Equilibrada</option>
                    <option value="LOWER_PURCHASE_COST">Menor coste real</option>
                    <option value="LOWER_WASTE">Menor desperdicio</option>
                    <option value="MORE_REUSE">Mayor reutilización útil</option>
                  </select>
                </Field>
              )}
              <p>
                El modo clásico ignora cualquier preset y conserva el algoritmo anterior. Compra
                eficiente valora envases completos, sobrantes y reutilización económicamente útil.
              </p>
            </FormSection>

            <FormSection
              title="4. Estructura"
              description="Tipos permitidos y preferencia de variedad."
            >
              <CheckboxGroup legend="Tipos de comida" error={errors.allowedMealTypes?.message}>
                {mealTypes.map(([value, label]) => (
                  <label key={value}>
                    <input type="checkbox" value={value} {...register('allowedMealTypes')} />
                    {label}
                  </label>
                ))}
              </CheckboxGroup>
              <Field label="Preferencia de variedad" error={errors.varietyPreference?.message}>
                <select {...register('varietyPreference')}>
                  <option value="LOW">Baja</option>
                  <option value="MEDIUM">Media</option>
                  <option value="HIGH">Alta</option>
                </select>
              </Field>
              <Field
                label="Máximo de repeticiones"
                error={errors.maximumTemplateRepetitions?.message}
              >
                <input type="number" min="1" {...register('maximumTemplateRepetitions')} />
              </Field>
              <Field
                label="Preparación máxima (min)"
                error={errors.maximumPreparationMinutes?.message}
              >
                <input type="number" min="1" {...register('maximumPreparationMinutes')} />
              </Field>
            </FormSection>

            <FormSection
              title="5. Restricciones"
              description="Se aplican antes de puntuar candidatos."
            >
              <CheckboxGroup legend="Etiquetas dietéticas requeridas">
                {dietaryTags.data?.map((tag) => (
                  <label key={tag.code}>
                    <input type="checkbox" value={tag.code} {...register('requiredDietaryTags')} />
                    {tag.name}
                  </label>
                ))}
              </CheckboxGroup>
              <CheckboxGroup legend="Alérgenos excluidos">
                {allergens.data?.map((allergen) => (
                  <label key={allergen.code}>
                    <input
                      type="checkbox"
                      value={allergen.code}
                      {...register('excludedAllergens')}
                    />
                    {allergen.name}
                  </label>
                ))}
              </CheckboxGroup>
              <label className={styles.switch}>
                <input type="checkbox" {...register('allowIncompleteCalculations')} />
                Permitir plantillas con cálculo incompleto
              </label>
            </FormSection>

            <FormSection
              title="6. Exclusiones"
              description="Opcional: evita plantillas o productos concretos."
            >
              <CheckboxGroup legend="Plantillas excluidas">
                {templateOptions.map((template) => (
                  <label key={template.id}>
                    <input
                      type="checkbox"
                      value={template.id}
                      {...register('excludedTemplateIds')}
                    />
                    {template.name}
                  </label>
                ))}
              </CheckboxGroup>
              <CheckboxGroup legend="Productos excluidos">
                {products.data?.content.map((product) => (
                  <label key={product.id}>
                    <input type="checkbox" value={product.id} {...register('excludedProductIds')} />
                    {product.name}
                  </label>
                ))}
              </CheckboxGroup>
            </FormSection>

            <FormSection
              title="7. Reproducibilidad"
              description="La seed es opcional; el servidor genera una si se deja vacía."
            >
              <Field label="Seed determinista" error={errors.deterministicSeed?.message}>
                <input type="number" {...register('deterministicSeed')} placeholder="Automática" />
              </Field>
            </FormSection>

            {requestError && (
              <p className={styles.error} role="alert">
                {requestError}
              </p>
            )}
            <div className={styles.submitBar}>
              <span>Los ingredientes opcionales no participan en los cálculos generados.</span>
              <button type="submit" disabled={generator.isPending}>
                {generator.isPending ? 'Generando…' : 'Generar previsualización'}
              </button>
            </div>
          </form>
        )}

        {preview && (
          <section className={styles.preview}>
            <header>
              <div>
                <span>Previsualización no guardada</span>
                <h2>{preview.name}</h2>
              </div>
              <div className={styles.actions}>
                <button
                  type="button"
                  onClick={() => void regenerate(true)}
                  disabled={generator.isPending}
                >
                  Misma seed
                </button>
                <button
                  type="button"
                  onClick={() => void regenerate(false)}
                  disabled={generator.isPending}
                >
                  Nueva seed
                </button>
                <button type="button" onClick={() => setPreview(undefined)}>
                  Modificar
                </button>
                <button type="button" onClick={() => navigate('/meal-plans')}>
                  Descartar
                </button>
                <button
                  className={styles.save}
                  type="button"
                  onClick={() => void savePreview()}
                  disabled={generator.isPending}
                >
                  Guardar este plan
                </button>
              </div>
            </header>
            {requestError && (
              <p className={styles.error} role="alert">
                {requestError}
              </p>
            )}
            <MealPlanResult plan={preview} />
          </section>
        )}
      </main>
    </div>
  )
}

function FormSection({
  title,
  description,
  children,
}: {
  title: string
  description: string
  children: React.ReactNode
}) {
  return (
    <section className={styles.section}>
      <header>
        <h2>{title}</h2>
        <p>{description}</p>
      </header>
      <div>{children}</div>
    </section>
  )
}

function Field({
  label,
  error,
  children,
}: {
  label: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <label className={styles.field}>
      <span>{label}</span>
      {children}
      {error && <small role="alert">{error}</small>}
    </label>
  )
}

function CheckboxGroup({
  legend,
  error,
  children,
}: {
  legend: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <fieldset className={styles.checks}>
      <legend>{legend}</legend>
      <div>{children}</div>
      {error && <small role="alert">{error}</small>}
    </fieldset>
  )
}

function toRequest(values: MealPlanFormValues, persist: boolean): GenerateMealPlanRequest {
  const optional = (value: string) => (value === '' ? undefined : Number(value))
  return {
    supermarketCode: values.supermarketCode,
    name: values.name.trim(),
    startDate: values.startDate,
    numberOfDays: Number(values.numberOfDays),
    mealsPerDay: Number(values.mealsPerDay),
    servings: Number(values.servings),
    dailyCaloriesTarget: Number(values.dailyCaloriesTarget),
    dailyProteinTarget: Number(values.dailyProteinTarget),
    weeklyBudget: optional(values.weeklyBudget),
    allowedMealTypes: values.allowedMealTypes,
    requiredDietaryTags: values.requiredDietaryTags,
    excludedAllergens: values.excludedAllergens,
    excludedTemplateIds: values.excludedTemplateIds,
    excludedProductIds: values.excludedProductIds,
    maximumPreparationMinutes: optional(values.maximumPreparationMinutes),
    maximumTemplateRepetitions: optional(values.maximumTemplateRepetitions),
    varietyPreference: values.varietyPreference,
    allowIncompleteCalculations: values.allowIncompleteCalculations,
    strategy: values.strategy,
    optimizationPreset:
      values.strategy === 'PURCHASE_AWARE_SCORING' ? values.optimizationPreset : undefined,
    deterministicSeed: optional(values.deterministicSeed),
    persist,
  }
}

function nextMonday(): string {
  const date = new Date()
  const delta = (8 - date.getDay()) % 7 || 7
  date.setDate(date.getDate() + delta)
  return date.toISOString().slice(0, 10)
}

function valuesFromPlan(plan: GeneratedMealPlan): MealPlanFormValues {
  return {
    supermarketCode: plan.supermarketCode,
    name: `${plan.name} (copia)`,
    startDate: plan.startDate,
    numberOfDays: String(plan.numberOfDays),
    mealsPerDay: String(plan.mealsPerDay),
    servings: String(plan.servings),
    dailyCaloriesTarget: String(plan.criteria.dailyCaloriesTarget),
    dailyProteinTarget: String(plan.criteria.dailyProteinTarget),
    weeklyBudget: plan.weeklyBudget === null ? '' : String(plan.weeklyBudget),
    maximumPreparationMinutes:
      plan.criteria.maximumPreparationMinutes === null
        ? ''
        : String(plan.criteria.maximumPreparationMinutes),
    maximumTemplateRepetitions: String(plan.criteria.maximumTemplateRepetitions),
    varietyPreference: plan.criteria.varietyPreference,
    allowedMealTypes: plan.criteria.allowedMealTypes,
    requiredDietaryTags: plan.criteria.requiredDietaryTags,
    excludedAllergens: plan.criteria.excludedAllergens,
    excludedTemplateIds: plan.criteria.excludedTemplateIds,
    excludedProductIds: plan.criteria.excludedProductIds,
    allowIncompleteCalculations: plan.criteria.allowIncompleteCalculations,
    strategy: plan.strategy,
    optimizationPreset: plan.generationMetadata.optimizationPreset ?? 'BALANCED',
    deterministicSeed: String(plan.seed),
  }
}
