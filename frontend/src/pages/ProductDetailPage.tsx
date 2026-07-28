import { Link, useParams } from 'react-router-dom'
import { SiteHeader } from '../components/SiteHeader'
import { usePriceHistory, useProduct } from '../hooks/useCatalogQueries'
import { formatDate, formatMoney, formatPackage } from '../utils/format'
import styles from './ProductDetailPage.module.css'

const presenceLabels: Record<string, string> = {
  CONTAINS: 'Contiene',
  MAY_CONTAIN: 'Puede contener',
  TRACES: 'Trazas',
  UNKNOWN: 'Presencia desconocida',
}

export function ProductDetailPage() {
  const { id } = useParams()
  const product = useProduct(id)
  const priceHistory = usePriceHistory(id)

  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.main}>
        <Link className={styles.back} to="/products">
          ← Volver al catálogo
        </Link>

        {product.isPending ? (
          <DetailState message="Cargando producto…" />
        ) : product.isError ? (
          <DetailState
            error
            message="No se ha encontrado el producto o el backend no está disponible."
          />
        ) : (
          <>
            <section className={styles.hero}>
              <div className={styles.visual} aria-label="Imagen no disponible">
                <span aria-hidden="true">{product.data.name.slice(0, 1)}</span>
                <small>Imagen no disponible</small>
              </div>
              <div className={styles.heroCopy}>
                <div className={styles.eyebrows}>
                  <span>{product.data.categoryName}</span>
                  <span>{product.data.supermarketName}</span>
                  <span>Datos de demostración</span>
                </div>
                <h1>{product.data.name}</h1>
                <p className={styles.brand}>{product.data.brand ?? 'Marca no indicada'}</p>
                <p>{product.data.description ?? 'Sin descripción disponible.'}</p>
                <div className={styles.priceRow}>
                  <div>
                    <strong>{formatMoney(product.data.currentPrice)}</strong>
                    <span>{formatMoney(product.data.unitPrice)} / unidad de referencia</span>
                  </div>
                  <div>
                    <strong>
                      {formatPackage(product.data.packageQuantity, product.data.packageUnit)}
                    </strong>
                    <span
                      className={product.data.available ? styles.available : styles.unavailable}
                    >
                      {product.data.available ? 'Disponible' : 'No disponible'}
                    </span>
                  </div>
                </div>
                <small className={styles.updated}>
                  Última actualización: {formatDate(product.data.lastSyncedAt)}
                </small>
              </div>
            </section>

            <div className={styles.detailGrid}>
              <section className={styles.panel} aria-labelledby="nutrition-heading">
                <div className={styles.panelHeading}>
                  <span>Valores por 100 g</span>
                  <h2 id="nutrition-heading">Información nutricional</h2>
                </div>
                {product.data.nutrition ? (
                  <dl className={styles.nutritionGrid}>
                    <NutritionValue
                      label="Energía"
                      value={`${product.data.nutrition.caloriesPer100g} kcal`}
                    />
                    <NutritionValue
                      label="Proteína"
                      value={`${product.data.nutrition.proteinPer100g} g`}
                    />
                    <NutritionValue
                      label="Carbohidratos"
                      value={`${product.data.nutrition.carbohydratesPer100g} g`}
                    />
                    <NutritionValue
                      label="Grasas"
                      value={`${product.data.nutrition.fatPer100g} g`}
                    />
                    <NutritionValue
                      label="Fibra"
                      value={`${product.data.nutrition.fiberPer100g} g`}
                    />
                    <NutritionValue
                      label="Azúcares"
                      value={`${product.data.nutrition.sugarPer100g} g`}
                    />
                    <NutritionValue label="Sal" value={`${product.data.nutrition.saltPer100g} g`} />
                  </dl>
                ) : (
                  <p className={styles.missing}>Información nutricional no disponible</p>
                )}
              </section>

              <section className={styles.panel} aria-labelledby="attributes-heading">
                <div className={styles.panelHeading}>
                  <span>Restricciones</span>
                  <h2 id="attributes-heading">Etiquetas y alérgenos</h2>
                </div>
                <h3>Etiquetas dietéticas</h3>
                <div className={styles.tags}>
                  {product.data.dietaryTags.map((tag) => (
                    <span key={tag.code}>{tag.name}</span>
                  ))}
                </div>
                <h3>Alérgenos</h3>
                {product.data.allergens.length ? (
                  <ul className={styles.allergens}>
                    {product.data.allergens.map((allergen) => (
                      <li key={allergen.code}>
                        <strong>{allergen.name}</strong>
                        <span>{presenceLabels[allergen.presenceType]}</span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className={styles.missing}>No se han declarado alérgenos en los datos demo.</p>
                )}
              </section>
            </div>

            <section
              className={`${styles.panel} ${styles.history}`}
              aria-labelledby="history-heading"
            >
              <div className={styles.panelHeading}>
                <span>Precios ficticios</span>
                <h2 id="history-heading">Histórico de precios</h2>
              </div>
              {priceHistory.isPending ? (
                <p>Cargando histórico…</p>
              ) : priceHistory.isError ? (
                <p role="alert">No se ha podido cargar el histórico.</p>
              ) : (
                <div className={styles.tableWrap}>
                  <table>
                    <thead>
                      <tr>
                        <th>Fecha</th>
                        <th>Precio</th>
                        <th>Precio unitario</th>
                      </tr>
                    </thead>
                    <tbody>
                      {priceHistory.data.map((entry) => (
                        <tr key={entry.id}>
                          <td>{formatDate(entry.recordedAt)}</td>
                          <td>{formatMoney(entry.price)}</td>
                          <td>{formatMoney(entry.unitPrice)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <p className={styles.disclaimer}>
                Este histórico es exclusivamente de demostración y no refleja precios reales.
              </p>
            </section>
          </>
        )}
      </main>
    </div>
  )
}

function NutritionValue({ label, value }: { label: string; value: string }) {
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
