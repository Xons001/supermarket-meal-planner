import { Link } from 'react-router-dom'
import type { Product } from '../../types/api'
import { formatMoney, formatPackage } from '../../utils/format'
import styles from './ProductCard.module.css'

interface ProductCardProps {
  product: Product
}

export function ProductCard({ product }: ProductCardProps) {
  return (
    <Link
      className={styles.cardLink}
      to={`/products/${product.id}`}
      aria-label={`Ver detalle de ${product.name}`}
    >
      <article className={styles.card}>
        <div className={styles.visual} aria-hidden="true">
          <span>{product.name.slice(0, 1).toUpperCase()}</span>
          <small>Imagen no disponible</small>
        </div>
        <div className={styles.body}>
          <div className={styles.meta}>
            <span>{product.categoryName}</span>
            <strong className={product.available ? styles.available : styles.unavailable}>
              {product.available ? 'Disponible' : 'No disponible'}
            </strong>
          </div>
          <h2>{product.name}</h2>
          <p className={styles.brand}>{product.brand ?? 'Marca no indicada'}</p>
          <div className={styles.tags}>
            {product.dietaryTags.slice(0, 3).map((tag) => (
              <span key={tag.code}>{tag.name}</span>
            ))}
          </div>
          <dl className={styles.nutrition}>
            <div>
              <dt>Energía</dt>
              <dd>
                {product.nutrition ? `${product.nutrition.caloriesPer100g} kcal` : 'No disponible'}
              </dd>
            </div>
            <div>
              <dt>Proteína</dt>
              <dd>
                {product.nutrition ? `${product.nutrition.proteinPer100g} g` : 'No disponible'}
              </dd>
            </div>
          </dl>
          <div className={styles.price}>
            <div>
              <strong>{formatMoney(product.currentPrice)}</strong>
              <small>{formatMoney(product.unitPrice)} / unidad de referencia</small>
            </div>
            <span>{formatPackage(product.packageQuantity, product.packageUnit)}</span>
          </div>
          <p className={styles.demo}>Precio ficticio · Datos de demostración</p>
        </div>
      </article>
    </Link>
  )
}
