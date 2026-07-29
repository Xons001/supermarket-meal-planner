import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useRef, useState } from 'react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router'
import { SiteHeader } from '../components/SiteHeader'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useProducts, useSupermarkets } from '../hooks/useCatalogQueries'
import {
  useCreateMealTemplate,
  useMealTemplate,
  usePreviewMealTemplate,
  useUpdateMealTemplate,
} from '../hooks/useMealTemplateQueries'
import { mealTemplateFormSchema, type MealTemplateFormValues } from '../schemas/mealTemplateForm'
import type { MealTemplate, MealTemplateRequest, Product, QuantityUnit } from '../types/api'
import { formatDecimal, formatMoney, formatPackage } from '../utils/format'
import styles from './MealTemplateFormPage.module.css'

const emptyIngredient = {
  productId: '',
  productLabel: '',
  quantity: '100',
  quantityUnit: 'GRAM' as QuantityUnit,
  optional: false,
  sortOrder: '0',
  notes: '',
}

const defaultValues: MealTemplateFormValues = {
  supermarketCode: 'MERCADONA',
  name: '',
  description: '',
  mealType: 'LUNCH',
  preparationMinutes: '20',
  servings: '1',
  active: true,
  instructions: [{ text: '' }],
  ingredients: [emptyIngredient],
}

export function MealTemplateFormPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const loadedTemplate = useMealTemplate(id)
  const createMutation = useCreateMealTemplate()
  const updateMutation = useUpdateMealTemplate(id ?? '')
  const previewMutation = usePreviewMealTemplate()
  const supermarkets = useSupermarkets()
  const loadedId = useRef<string | undefined>(undefined)
  const [successMessage, setSuccessMessage] = useState('')

  const {
    register,
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<MealTemplateFormValues>({
    resolver: zodResolver(mealTemplateFormSchema),
    defaultValues,
  })
  const instructions = useFieldArray({ control, name: 'instructions' })
  const ingredients = useFieldArray({ control, name: 'ingredients' })
  const supermarketCode = useWatch({ control, name: 'supermarketCode' })
  const ingredientValues = useWatch({ control, name: 'ingredients' })

  useEffect(() => {
    if (!loadedTemplate.data || loadedId.current === loadedTemplate.data.id) return
    reset(toFormValues(loadedTemplate.data))
    loadedId.current = loadedTemplate.data.id
  }, [loadedTemplate.data, reset])

  async function save(values: MealTemplateFormValues) {
    setSuccessMessage('')
    const request = toRequest(values)
    const saved = editing
      ? await updateMutation.mutateAsync(request)
      : await createMutation.mutateAsync(request)
    setSuccessMessage('Plantilla guardada correctamente.')
    navigate(`/meal-templates/${saved.id}`)
  }

  async function calculate(values: MealTemplateFormValues) {
    setSuccessMessage('')
    await previewMutation.mutateAsync(toRequest(values))
  }

  const mutationError =
    createMutation.error ?? updateMutation.error ?? previewMutation.error ?? loadedTemplate.error
  const isSubmitting = createMutation.isPending || updateMutation.isPending

  if (editing && loadedTemplate.isPending) {
    return (
      <div className={styles.page}>
        <SiteHeader />
        <main className={styles.main}>
          <p role="status">Cargando plantilla…</p>
        </main>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.main}>
        <Link className={styles.back} to={editing ? `/meal-templates/${id}` : '/meal-templates'}>
          ← Volver
        </Link>
        <div className={styles.heading}>
          <div>
            <span>FASE 2 · Editor de plantillas</span>
            <h1>{editing ? 'Editar plantilla' : 'Crear plantilla'}</h1>
            <p>
              Selecciona productos del catálogo y calcula valores antes de guardar. Todos los datos
              son de demostración.
            </p>
          </div>
        </div>

        <form className={styles.form} onSubmit={handleSubmit(save)} noValidate>
          <section className={styles.panel}>
            <h2>Datos generales</h2>
            <div className={styles.formGrid}>
              <Field label="Supermercado" error={errors.supermarketCode?.message}>
                <select {...register('supermarketCode')}>
                  {supermarkets.data?.map((supermarket) => (
                    <option
                      key={supermarket.code}
                      value={supermarket.code}
                      disabled={!supermarket.enabled}
                    >
                      {supermarket.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Tipo de comida" error={errors.mealType?.message}>
                <select {...register('mealType')}>
                  <option value="BREAKFAST">Desayuno</option>
                  <option value="LUNCH">Comida</option>
                  <option value="SNACK">Merienda</option>
                  <option value="DINNER">Cena</option>
                </select>
              </Field>
              <Field label="Nombre" error={errors.name?.message} wide>
                <input {...register('name')} placeholder="Ej. Arroz con pollo" />
              </Field>
              <Field label="Descripción" error={errors.description?.message} wide>
                <textarea {...register('description')} rows={3} />
              </Field>
              <Field label="Tiempo de preparación (min)" error={errors.preparationMinutes?.message}>
                <input type="number" min="0" {...register('preparationMinutes')} />
              </Field>
              <Field label="Raciones" error={errors.servings?.message}>
                <input type="number" min="1" {...register('servings')} />
              </Field>
              <label className={styles.checkbox}>
                <input type="checkbox" {...register('active')} />
                <span>Plantilla activa</span>
              </label>
            </div>
          </section>

          <section className={styles.panel}>
            <div className={styles.sectionHeading}>
              <div>
                <h2>Instrucciones</h2>
                <p>Se almacenan como una colección ordenada.</p>
              </div>
              <button type="button" onClick={() => instructions.append({ text: '' })}>
                Añadir paso
              </button>
            </div>
            <div className={styles.rows}>
              {instructions.fields.map((field, index) => (
                <div className={styles.instructionRow} key={field.id}>
                  <span>{index + 1}</span>
                  <div>
                    <input
                      aria-label={`Instrucción ${index + 1}`}
                      {...register(`instructions.${index}.text`)}
                      placeholder="Describe este paso"
                    />
                    <ErrorText message={errors.instructions?.[index]?.text?.message} />
                  </div>
                  <button
                    type="button"
                    disabled={instructions.fields.length === 1}
                    onClick={() => instructions.remove(index)}
                  >
                    Eliminar
                  </button>
                </div>
              ))}
            </div>
          </section>

          <section className={styles.panel}>
            <div className={styles.sectionHeading}>
              <div>
                <h2>Ingredientes</h2>
                <p>La búsqueda remota consulta como máximo seis productos cada vez.</p>
              </div>
              <button
                type="button"
                onClick={() =>
                  ingredients.append({
                    ...emptyIngredient,
                    sortOrder: String(ingredients.fields.length),
                  })
                }
              >
                Añadir ingrediente
              </button>
            </div>
            <ErrorText
              message={
                typeof errors.ingredients?.message === 'string'
                  ? errors.ingredients.message
                  : errors.ingredients?.root?.message
              }
            />
            <div className={styles.ingredientRows}>
              {ingredients.fields.map((field, index) => (
                <article key={field.id} className={styles.ingredientRow}>
                  <div className={styles.ingredientTop}>
                    <strong>Ingrediente {index + 1}</strong>
                    <button
                      type="button"
                      disabled={ingredients.fields.length === 1}
                      onClick={() => ingredients.remove(index)}
                    >
                      Eliminar
                    </button>
                  </div>
                  <input type="hidden" {...register(`ingredients.${index}.productId`)} />
                  <input type="hidden" {...register(`ingredients.${index}.productLabel`)} />
                  <ProductSelector
                    index={index}
                    supermarketCode={supermarketCode}
                    selectedLabel={ingredientValues[index]?.productLabel ?? ''}
                    onSelect={(product) => {
                      setValue(`ingredients.${index}.productId`, product.id, {
                        shouldValidate: true,
                      })
                      setValue(`ingredients.${index}.productLabel`, product.name)
                      setValue(
                        `ingredients.${index}.quantityUnit`,
                        unitForMeasurement(product.measurementType),
                        { shouldValidate: true },
                      )
                    }}
                  />
                  <ErrorText message={errors.ingredients?.[index]?.productId?.message} />
                  <div className={styles.ingredientGrid}>
                    <Field label="Cantidad" error={errors.ingredients?.[index]?.quantity?.message}>
                      <input
                        type="number"
                        min="0.001"
                        step="0.001"
                        {...register(`ingredients.${index}.quantity`)}
                      />
                    </Field>
                    <Field
                      label="Unidad"
                      error={errors.ingredients?.[index]?.quantityUnit?.message}
                    >
                      <select {...register(`ingredients.${index}.quantityUnit`)}>
                        <option value="GRAM">Gramos</option>
                        <option value="MILLILITER">Mililitros</option>
                        <option value="UNIT">Unidades</option>
                      </select>
                    </Field>
                    <Field label="Orden" error={errors.ingredients?.[index]?.sortOrder?.message}>
                      <input
                        type="number"
                        min="0"
                        {...register(`ingredients.${index}.sortOrder`)}
                      />
                    </Field>
                    <label className={styles.checkbox}>
                      <input type="checkbox" {...register(`ingredients.${index}.optional`)} />
                      <span>Ingrediente opcional</span>
                    </label>
                    <Field label="Notas" error={errors.ingredients?.[index]?.notes?.message} wide>
                      <input {...register(`ingredients.${index}.notes`)} placeholder="Opcional" />
                    </Field>
                  </div>
                </article>
              ))}
            </div>
          </section>

          {mutationError && (
            <p className={styles.apiError} role="alert">
              {mutationError.message}
            </p>
          )}
          {successMessage && <p className={styles.success}>{successMessage}</p>}

          <div className={styles.submitRow}>
            <button
              className={styles.secondary}
              type="button"
              disabled={previewMutation.isPending}
              onClick={handleSubmit(calculate)}
            >
              {previewMutation.isPending ? 'Calculando…' : 'Calcular previsualización'}
            </button>
            <button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear plantilla'}
            </button>
          </div>
        </form>

        {previewMutation.data && <Preview template={previewMutation.data} />}
      </main>
    </div>
  )
}

function ProductSelector({
  index,
  supermarketCode,
  selectedLabel,
  onSelect,
}: {
  index: number
  supermarketCode: string
  selectedLabel: string
  onSelect: (product: Product) => void
}) {
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query, 400)
  const products = useProducts(
    {
      supermarketCode,
      query: debouncedQuery.trim(),
      page: 0,
      size: 6,
      sort: 'name,asc',
    },
    debouncedQuery.trim().length >= 2,
  )

  return (
    <div className={styles.productSelector}>
      <label>
        <span>Buscar y seleccionar producto</span>
        <input
          type="search"
          aria-label={`Buscar producto del ingrediente ${index + 1}`}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Escribe al menos 2 caracteres"
        />
      </label>
      {selectedLabel && <p>Seleccionado: {selectedLabel}</p>}
      {products.isFetching && <small>Buscando…</small>}
      {products.data && query.trim().length >= 2 && (
        <div className={styles.productResults}>
          {products.data.content.length === 0 ? (
            <small>Sin resultados</small>
          ) : (
            products.data.content.map((product) => (
              <button
                key={product.id}
                type="button"
                onClick={() => {
                  onSelect(product)
                  setQuery('')
                }}
              >
                <strong>{product.name}</strong>
                <span>
                  {product.brand ?? 'Sin marca'} ·{' '}
                  {formatPackage(product.packageQuantity, product.packageUnit)} ·{' '}
                  {formatMoney(product.currentPrice)} ·{' '}
                  {product.available ? 'Disponible' : 'No disponible'}
                </span>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  )
}

function Preview({ template }: { template: MealTemplate }) {
  return (
    <section className={styles.preview} aria-labelledby="preview-heading">
      <div>
        <span>Sin persistir</span>
        <h2 id="preview-heading">Previsualización del cálculo</h2>
      </div>
      <dl>
        <PreviewValue
          label="Calorías totales"
          value={formatDecimal(template.totalNutrition.calories, ' kcal')}
        />
        <PreviewValue
          label="Proteína total"
          value={formatDecimal(template.totalNutrition.protein, ' g')}
        />
        <PreviewValue
          label="Carbohidratos"
          value={formatDecimal(template.totalNutrition.carbohydrates, ' g')}
        />
        <PreviewValue label="Grasas" value={formatDecimal(template.totalNutrition.fat, ' g')} />
        <PreviewValue label="Coste consumido" value={formatMoney(template.totalConsumedCost)} />
      </dl>
      {template.warnings.length > 0 ? (
        <ul>
          {template.warnings.map((warning) => (
            <li key={warning}>{warning}</li>
          ))}
        </ul>
      ) : (
        <p>Cálculo completo, sin advertencias.</p>
      )}
    </section>
  )
}

function PreviewValue({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

function Field({
  label,
  error,
  wide = false,
  children,
}: {
  label: string
  error?: string
  wide?: boolean
  children: React.ReactNode
}) {
  return (
    <label className={`${styles.field} ${wide ? styles.wide : ''}`}>
      <span>{label}</span>
      {children}
      <ErrorText message={error} />
    </label>
  )
}

function ErrorText({ message }: { message?: string }) {
  return message ? <small className={styles.fieldError}>{message}</small> : null
}

function toRequest(values: MealTemplateFormValues): MealTemplateRequest {
  return {
    supermarketCode: values.supermarketCode,
    name: values.name.trim(),
    description: values.description.trim(),
    mealType: values.mealType,
    instructions: values.instructions.map((instruction) => instruction.text.trim()),
    preparationMinutes: Number(values.preparationMinutes),
    servings: Number(values.servings),
    active: values.active,
    ingredients: values.ingredients.map((ingredient) => ({
      productId: ingredient.productId,
      quantity: Number(ingredient.quantity),
      quantityUnit: ingredient.quantityUnit,
      optional: ingredient.optional,
      sortOrder: Number(ingredient.sortOrder),
      notes: ingredient.notes.trim() || null,
    })),
  }
}

function toFormValues(template: MealTemplate): MealTemplateFormValues {
  return {
    supermarketCode: template.supermarketCode,
    name: template.name,
    description: template.description,
    mealType: template.mealType,
    preparationMinutes: String(template.preparationMinutes),
    servings: String(template.servings),
    active: template.active,
    instructions: template.instructions.map((text) => ({ text })),
    ingredients: template.ingredients.map((ingredient) => ({
      productId: ingredient.productId,
      productLabel: ingredient.productName,
      quantity: String(ingredient.quantity),
      quantityUnit: ingredient.quantityUnit,
      optional: ingredient.optional,
      sortOrder: String(ingredient.sortOrder),
      notes: ingredient.notes ?? '',
    })),
  }
}

function unitForMeasurement(measurementType: Product['measurementType']): QuantityUnit {
  return measurementType === 'WEIGHT'
    ? 'GRAM'
    : measurementType === 'VOLUME'
      ? 'MILLILITER'
      : 'UNIT'
}
