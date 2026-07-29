import { Link, useNavigate, useParams } from 'react-router'
import { SiteHeader } from '../components/SiteHeader'
import {
  useArchiveMealTemplate,
  useChangeMealTemplateStatus,
  useMealTemplate,
} from '../hooks/useMealTemplateQueries'
import type { NutritionBreakdown } from '../types/api'
import {
  formatDate,
  formatDecimal,
  formatMoney,
  mealTypeLabels,
  quantityUnitLabels,
} from '../utils/format'
import styles from './MealTemplateDetailPage.module.css'
import { useAuth } from '../auth/AuthProvider'

export function MealTemplateDetailPage() {
  const { user } = useAuth()
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const template = useMealTemplate(id)
  const statusMutation = useChangeMealTemplateStatus(id)
  const archiveMutation = useArchiveMealTemplate(id)

  async function handleArchive() {
    if (!window.confirm('¿Archivar esta plantilla? Dejará de aparecer en el listado.')) return
    await archiveMutation.mutateAsync()
    navigate('/meal-templates')
  }

  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.main}>
        <Link className={styles.back} to="/meal-templates">
          ← Volver a plantillas
        </Link>
        {template.isPending ? (
          <DetailState message="Cargando plantilla…" />
        ) : template.isError ? (
          <DetailState error message="No se ha encontrado la plantilla." />
        ) : (
          <>
            <section className={styles.hero}>
              <div>
                <div className={styles.badges}>
                  <span>{mealTypeLabels[template.data.mealType]}</span>
                  <span>{template.data.supermarketName}</span>
                  <span>Datos de demostración</span>
                </div>
                <h1>{template.data.name}</h1>
                <p>{template.data.description}</p>
                <div className={styles.meta}>
                  <strong>{template.data.preparationMinutes} min</strong>
                  <strong>
                    {template.data.servings} {template.data.servings === 1 ? 'ración' : 'raciones'}
                  </strong>
                  <strong className={template.data.active ? styles.active : styles.inactive}>
                    {template.data.active ? 'Activa' : 'Desactivada'}
                  </strong>
                </div>
                <small>
                  Creada {formatDate(template.data.createdAt)} · Actualizada{' '}
                  {formatDate(template.data.updatedAt)}
                </small>
              </div>
              <div className={styles.actions}>
                {user?.role === 'ADMIN' && <Link to={`/meal-templates/${id}/edit`}>Editar</Link>}
                {user?.role === 'ADMIN' && (
                  <button
                    type="button"
                    disabled={statusMutation.isPending}
                    onClick={() => statusMutation.mutate(!template.data.active)}
                  >
                    {template.data.active ? 'Desactivar' : 'Activar'}
                  </button>
                )}
                <button
                  className={styles.danger}
                  type="button"
                  disabled={archiveMutation.isPending}
                  onClick={handleArchive}
                >
                  Archivar
                </button>
              </div>
            </section>

            {template.data.warnings.length > 0 && (
              <section className={styles.warnings} aria-labelledby="warnings-heading">
                <h2 id="warnings-heading">Advertencias del cálculo</h2>
                <ul>
                  {template.data.warnings.map((warning) => (
                    <li key={warning}>{warning}</li>
                  ))}
                </ul>
              </section>
            )}

            <div className={styles.summaryGrid}>
              <NutritionPanel
                title="Valores totales"
                nutrition={template.data.totalNutrition}
                cost={template.data.totalConsumedCost}
              />
              <NutritionPanel
                title="Valores por ración"
                nutrition={template.data.nutritionPerServing}
                cost={template.data.consumedCostPerServing}
              />
            </div>

            <section className={styles.panel}>
              <span className={styles.eyebrow}>Preparación</span>
              <h2>Instrucciones</h2>
              <ol className={styles.instructions}>
                {template.data.instructions.map((instruction, index) => (
                  <li key={`${index}-${instruction}`}>{instruction}</li>
                ))}
              </ol>
            </section>

            <section className={styles.panel}>
              <span className={styles.eyebrow}>Cantidades y cálculo proporcional</span>
              <h2>Ingredientes</h2>
              <div className={styles.ingredients}>
                {template.data.ingredients.map((ingredient) => (
                  <article key={ingredient.productId}>
                    <div className={styles.ingredientHeading}>
                      <div>
                        <h3>{ingredient.productName}</h3>
                        <p>
                          {ingredient.brand ?? 'Marca no indicada'} · {ingredient.category}
                        </p>
                      </div>
                      {ingredient.optional && <span>Opcional</span>}
                    </div>
                    <dl>
                      <Metric
                        label="Cantidad"
                        value={`${ingredient.quantity} ${quantityUnitLabels[ingredient.quantityUnit]}`}
                      />
                      <Metric
                        label="Calorías"
                        value={
                          ingredient.calculatedNutrition
                            ? formatDecimal(ingredient.calculatedNutrition.calories, ' kcal')
                            : 'No disponible'
                        }
                      />
                      <Metric
                        label="Proteína"
                        value={
                          ingredient.calculatedNutrition
                            ? formatDecimal(ingredient.calculatedNutrition.protein, ' g')
                            : 'No disponible'
                        }
                      />
                      <Metric
                        label="Coste consumido"
                        value={
                          ingredient.calculatedConsumedCost === null
                            ? 'No disponible'
                            : formatMoney(ingredient.calculatedConsumedCost)
                        }
                      />
                    </dl>
                    {ingredient.notes && <p className={styles.notes}>{ingredient.notes}</p>}
                    {ingredient.warnings.length > 0 && (
                      <ul className={styles.ingredientWarnings}>
                        {ingredient.warnings.map((warning) => (
                          <li key={warning}>{warning}</li>
                        ))}
                      </ul>
                    )}
                  </article>
                ))}
              </div>
            </section>

            <p className={styles.disclaimer}>
              El coste mostrado es el valor proporcional consumido. No es el coste de comprar
              paquetes enteros y todos los datos son ficticios.
            </p>
          </>
        )}
      </main>
    </div>
  )
}

function NutritionPanel({
  title,
  nutrition,
  cost,
}: {
  title: string
  nutrition: NutritionBreakdown
  cost: number
}) {
  return (
    <section className={styles.panel}>
      <span className={styles.eyebrow}>Nutrición y coste consumido</span>
      <h2>{title}</h2>
      <dl className={styles.nutrition}>
        <Metric label="Energía" value={formatDecimal(nutrition.calories, ' kcal')} />
        <Metric label="Proteína" value={formatDecimal(nutrition.protein, ' g')} />
        <Metric label="Carbohidratos" value={formatDecimal(nutrition.carbohydrates, ' g')} />
        <Metric label="Grasas" value={formatDecimal(nutrition.fat, ' g')} />
        <Metric label="Fibra" value={formatDecimal(nutrition.fiber, ' g')} />
        <Metric label="Azúcares" value={formatDecimal(nutrition.sugar, ' g')} />
        <Metric label="Sal" value={formatDecimal(nutrition.salt, ' g')} />
        <Metric label="Coste" value={formatMoney(cost)} />
      </dl>
    </section>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

function DetailState({ message, error = false }: { message: string; error?: boolean }) {
  return (
    <div
      className={`${styles.state} ${error ? styles.error : ''}`}
      role={error ? 'alert' : 'status'}
    >
      {message}
    </div>
  )
}
