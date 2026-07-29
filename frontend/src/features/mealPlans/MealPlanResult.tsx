import { Link } from 'react-router'
import type { GeneratedMealPlan, MealPlanWarning } from '../../types/mealPlan'
import { formatDecimal, formatMoney } from '../../utils/format'
import styles from './MealPlanResult.module.css'

const mealTypeLabels = {
  BREAKFAST: 'Desayuno',
  LUNCH: 'Comida',
  SNACK: 'Merienda',
  DINNER: 'Cena',
}

export function MealPlanResult({ plan }: { plan: GeneratedMealPlan }) {
  const purchase = plan.purchaseMetrics
  return (
    <div className={styles.result}>
      <section className={styles.summary} aria-label="Resumen del plan">
        <Metric label="Puntuación" value={`${formatDecimal(plan.overallScore)} / 100`} strong />
        <Metric label="Coste consumido" value={formatMoney(plan.totalConsumedCost)} />
        <Metric
          label="Calorías semanales"
          value={`${formatDecimal(plan.weeklyNutrition.calories)} kcal`}
        />
        <Metric
          label="Proteína semanal"
          value={`${formatDecimal(plan.weeklyNutrition.protein)} g`}
        />
        <Metric label="Variedad" value={`${plan.varietyMetrics.uniqueTemplates} plantillas`} />
        <Metric label="Generación" value={`${plan.generationMetadata.durationMilliseconds} ms`} />
      </section>

      <div className={styles.notice}>
        {purchase ? (
          <>
            <strong>Dos costes distintos:</strong> el coste consumido representa solo los
            ingredientes utilizados; el coste real estima los envases completos que hay que comprar.
          </>
        ) : (
          <>
            <strong>Plan clásico o histórico:</strong> solo conserva el coste proporcional
            consumido, no una estimación de envases completos.
          </>
        )}
      </div>

      {purchase && (
        <section className={styles.purchase} aria-label="Optimización de compra">
          <header>
            <div>
              <span>Compra estimada</span>
              <h2>{formatMoney(purchase.estimatedPurchaseCost)}</h2>
            </div>
            <strong data-budget={purchase.purchaseBudgetExceeded ? 'exceeded' : 'within'}>
              {budgetLabel(purchase.purchaseBudgetDifference)}
            </strong>
          </header>
          <dl>
            <PurchaseMetric
              label="Ingredientes utilizados"
              value={formatMoney(purchase.estimatedConsumedCost)}
            />
            <PurchaseMetric
              label="Sobrantes estimados"
              value={formatMoney(purchase.estimatedWasteCost)}
            />
            <PurchaseMetric
              label="Desperdicio"
              value={`${formatDecimal(purchase.estimatedWastePercentage)} %`}
            />
            <PurchaseMetric label="Envases" value={String(purchase.estimatedPackageCount)} />
            <PurchaseMetric
              label="Productos únicos"
              value={String(purchase.estimatedUniqueProductCount)}
            />
            <PurchaseMetric
              label="Reutilización útil"
              value={`${purchase.economicallyUsefulReuseCount} mejoras`}
            />
          </dl>
          {!purchase.calculationComplete && (
            <p role="status">
              Estimación parcial: algunos productos no tienen snapshots completos de precio, envase,
              unidad o disponibilidad.
            </p>
          )}
          <div className={styles.reasons}>
            <h3>Por qué se eligió</h3>
            <ul>
              {purchase.selectionReasons.map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          </div>
        </section>
      )}

      {plan.warnings.length > 0 && (
        <section className={styles.warnings} aria-label="Advertencias">
          <h2>Advertencias y explicaciones</h2>
          <ul>
            {plan.warnings.map((warning, index) => (
              <Warning
                key={`${warning.code}-${warning.dayIndex ?? 'plan'}-${index}`}
                value={warning}
              />
            ))}
          </ul>
        </section>
      )}

      <section className={styles.days} aria-label="Días del plan">
        {plan.days.map((day) => (
          <details key={day.date} open={day.dayIndex === 0}>
            <summary>
              <span>
                Día {day.dayIndex + 1} ·{' '}
                {new Intl.DateTimeFormat('es-ES', {
                  weekday: 'long',
                  day: 'numeric',
                  month: 'short',
                  timeZone: 'UTC',
                }).format(new Date(`${day.date}T00:00:00Z`))}
              </span>
              <small>
                {formatDecimal(day.totalNutrition.calories)} kcal ·{' '}
                {formatDecimal(day.totalNutrition.protein)} g proteína ·{' '}
                {formatMoney(day.totalConsumedCost)}
              </small>
            </summary>
            <div className={styles.meals}>
              {day.meals.map((meal) => (
                <article key={`${day.date}-${meal.position}`}>
                  <div className={styles.mealHeading}>
                    <span>{mealTypeLabels[meal.mealType]}</span>
                    <strong>
                      <Link to={`/meal-templates/${meal.templateId}`}>{meal.templateName}</Link>
                    </strong>
                    <small>{meal.preparationMinutes} min</small>
                  </div>
                  <dl>
                    <div>
                      <dt>Nutrición</dt>
                      <dd>
                        {formatDecimal(meal.nutrition.calories)} kcal ·{' '}
                        {formatDecimal(meal.nutrition.protein)} g proteína
                      </dd>
                    </div>
                    <div>
                      <dt>Coste consumido</dt>
                      <dd>{formatMoney(meal.consumedCost)}</dd>
                    </div>
                    <div>
                      <dt>Score</dt>
                      <dd>{formatDecimal(meal.score)}</dd>
                    </div>
                  </dl>
                  <details className={styles.ingredients}>
                    <summary>Ver ingredientes obligatorios</summary>
                    <ul>
                      {meal.ingredients.map((ingredient) => (
                        <li key={ingredient.productId}>
                          {ingredient.productName}: {formatDecimal(ingredient.quantity)}{' '}
                          {ingredient.quantityUnit.toLowerCase()}
                        </li>
                      ))}
                    </ul>
                  </details>
                </article>
              ))}
            </div>
          </details>
        ))}
      </section>

      <section className={styles.analysis}>
        <div>
          <h2>Desglose de puntuación</h2>
          <dl>
            {Object.entries(plan.scoreBreakdown)
              .filter((entry): entry is [string, number] => typeof entry[1] === 'number')
              .map(([key, value]) => (
                <div key={key}>
                  <dt>{scoreLabel(key)}</dt>
                  <dd>{formatDecimal(value)}</dd>
                </div>
              ))}
          </dl>
        </div>
        <div>
          <h2>Restricciones</h2>
          <h3>Aplicadas</h3>
          <ul>
            {plan.constraintsApplied.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
          {plan.constraintsNotMet.length > 0 && (
            <>
              <h3>No alcanzadas por la mejor solución viable</h3>
              <ul>
                {plan.constraintsNotMet.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </>
          )}
        </div>
      </section>

      <footer className={styles.metadata}>
        Algoritmo {plan.generationMetadata.algorithmVersion} · seed {plan.seed} ·{' '}
        {plan.generationMetadata.candidatesEvaluated} candidatos evaluados
      </footer>
    </div>
  )
}

function PurchaseMetric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

function budgetLabel(difference: number | null): string {
  if (difference === null) return 'Sin presupuesto configurado'
  return difference >= 0
    ? `${formatMoney(difference)} de margen`
    : `${formatMoney(Math.abs(difference))} sobre presupuesto`
}

function Metric({
  label,
  value,
  strong = false,
}: {
  label: string
  value: string
  strong?: boolean
}) {
  return (
    <div className={strong ? styles.primaryMetric : ''}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function Warning({ value }: { value: MealPlanWarning }) {
  return (
    <li data-severity={value.severity}>
      <strong>{value.severity}</strong>
      <span>{value.message}</span>
    </li>
  )
}

function scoreLabel(key: string): string {
  return (
    {
      calorieScore: 'Calorías',
      proteinScore: 'Proteína',
      budgetScore: 'Presupuesto',
      varietyScore: 'Variedad',
      repetitionScore: 'Repetición',
      completenessScore: 'Completitud',
      preparationScore: 'Preparación',
      purchaseCostScore: 'Coste real de compra',
      consumedCostScore: 'Coste consumido',
      purchaseBudgetScore: 'Presupuesto de compra',
      wasteCostScore: 'Coste de sobrantes',
      wastePercentageScore: 'Desperdicio porcentual',
      usefulReuseScore: 'Reutilización útil',
      uniqueProductsScore: 'Productos únicos',
      packageCountScore: 'Número de envases',
      totalScore: 'Total',
    }[key] ?? key
  )
}
