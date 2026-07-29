import { Link } from 'react-router'
import type { MealTemplate } from '../../types/api'
import { formatDecimal, formatMoney, mealTypeLabels } from '../../utils/format'
import styles from './MealTemplateCard.module.css'

export function MealTemplateCard({ template }: { template: MealTemplate }) {
  return (
    <Link
      className={styles.card}
      to={`/meal-templates/${template.id}`}
      aria-label={`Ver plantilla ${template.name}`}
    >
      <div className={styles.topline}>
        <span>{mealTypeLabels[template.mealType]}</span>
        <strong className={template.active ? styles.active : styles.inactive}>
          {template.active ? 'Activa' : 'Desactivada'}
        </strong>
      </div>
      <h3>{template.name}</h3>
      <p>{template.description}</p>
      <div className={styles.meta}>
        <span>{template.preparationMinutes} min</span>
        <span>
          {template.servings} {template.servings === 1 ? 'ración' : 'raciones'}
        </span>
      </div>
      <ul className={styles.ingredients}>
        {template.ingredients.slice(0, 3).map((ingredient) => (
          <li key={ingredient.productId}>
            {ingredient.productName}
            {ingredient.optional ? ' (opcional)' : ''}
          </li>
        ))}
      </ul>
      <dl className={styles.values}>
        <div>
          <dt>Por ración</dt>
          <dd>{formatDecimal(template.nutritionPerServing.calories, ' kcal')}</dd>
        </div>
        <div>
          <dt>Proteína</dt>
          <dd>{formatDecimal(template.nutritionPerServing.protein, ' g')}</dd>
        </div>
        <div>
          <dt>Coste consumido</dt>
          <dd>{formatMoney(template.consumedCostPerServing)}</dd>
        </div>
      </dl>
      {!template.calculationComplete && (
        <p className={styles.warning}>Cálculo parcial · revisa las advertencias</p>
      )}
      <small>Datos de demostración</small>
    </Link>
  )
}
