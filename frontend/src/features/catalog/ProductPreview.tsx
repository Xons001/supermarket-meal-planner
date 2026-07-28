import { useProducts } from '../../hooks/useCatalogQueries'
import styles from './ProductPreview.module.css'

interface ProductPreviewProps {
  supermarketCode: string | undefined
}

const unitLabels: Record<string, string> = {
  G: 'g',
  KG: 'kg',
  ML: 'ml',
  L: 'l',
  UNIT: 'uds.',
}

const formatMoney = new Intl.NumberFormat('es-ES', {
  style: 'currency',
  currency: 'EUR',
})

export function ProductPreview({ supermarketCode }: ProductPreviewProps) {
  const products = useProducts(supermarketCode)

  if (!supermarketCode) {
    return null
  }

  if (products.isPending) {
    return (
      <div className={styles.state} aria-live="polite">
        Cargando catálogo controlado…
      </div>
    )
  }

  if (products.isError) {
    return (
      <div className={`${styles.state} ${styles.error}`} role="alert">
        No se ha podido cargar el catálogo. Comprueba la conexión con el backend.
      </div>
    )
  }

  return (
    <section className={styles.section} id="catalogo" aria-labelledby="catalog-heading">
      <div className={styles.headingRow}>
        <div>
          <span className={styles.eyebrow}>Vertical slice operativo</span>
          <h2 id="catalog-heading">Catálogo de demostración</h2>
          <p>
            {products.data.totalElements} productos normalizados, con precio estimado y nutrición
            por 100 g.
          </p>
        </div>
        <span className={styles.demoBadge}>Datos de demostración</span>
      </div>

      <div className={styles.grid}>
        {products.data.content.slice(0, 6).map((product) => (
          <article className={styles.card} key={product.id}>
            <div className={styles.cardTop}>
              <span className={styles.category}>{product.categoryName}</span>
              <span className={styles.availability}>
                {product.available ? 'Disponible' : 'No disponible'}
              </span>
            </div>
            <h3>{product.name}</h3>
            <p className={styles.brand}>{product.brand}</p>
            <dl className={styles.metrics}>
              <div>
                <dt>Proteína</dt>
                <dd>{product.nutrition?.proteinPer100g ?? '—'} g</dd>
              </div>
              <div>
                <dt>Energía</dt>
                <dd>{product.nutrition?.caloriesPer100g ?? '—'} kcal</dd>
              </div>
            </dl>
            <div className={styles.priceRow}>
              <strong>{formatMoney.format(product.currentPrice)}</strong>
              <span>
                {product.packageQuantity} {unitLabels[product.packageUnit] ?? product.packageUnit}
              </span>
            </div>
            <small>Precio ficticio · no representa un precio actual</small>
          </article>
        ))}
      </div>
    </section>
  )
}
