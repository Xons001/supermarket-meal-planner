import { useState } from 'react'
import { Link } from 'react-router'
import { SiteHeader } from '../components/SiteHeader'
import { ProductPreview } from '../features/catalog/ProductPreview'
import { SupermarketSelector } from '../features/supermarkets/SupermarketSelector'
import { useSupermarkets } from '../hooks/useCatalogQueries'
import styles from './HomePage.module.css'

const workflowSteps = [
  {
    number: '01',
    title: 'Configura tus objetivos',
    description: 'Calorías, proteína, presupuesto, restricciones y número de comidas.',
  },
  {
    number: '02',
    title: 'Genera un plan comprobable',
    description: 'El motor determinista combina alimentos, nutrición y paquetes completos.',
  },
  {
    number: '03',
    title: 'Compra con menos desperdicio',
    description: 'La lista calcula paquetes, coste estimado, cantidad utilizada y sobrantes.',
  },
]

export function HomePage() {
  const supermarkets = useSupermarkets()
  const [selectedCode, setSelectedCode] = useState<string>()
  const activeSupermarketCode =
    selectedCode ?? supermarkets.data?.find((supermarket) => supermarket.enabled)?.code

  return (
    <div className={styles.page}>
      <SiteHeader />

      <main>
        <section className={styles.hero} id="inicio">
          <div className={styles.heroCopy}>
            <span className={styles.kicker}>Planificación práctica, datos comprobables</span>
            <h1>
              Tu semana de comidas,
              <span> conectada al supermercado.</span>
            </h1>
            <p>
              Una plataforma independiente para convertir objetivos nutricionales y presupuesto en
              un menú semanal basado en productos concretos, paquetes y precios estimados.
            </p>
            <div className={styles.heroActions}>
              <Link className={styles.primaryAction} to="/products">
                Explorar catálogo
              </Link>
              <a className={styles.secondaryAction} href="#como-funcionara">
                Ver cómo funcionará
              </a>
            </div>
            <div className={styles.trustRow}>
              <span>Sin IA para cálculos</span>
              <span>Precios por paquete</span>
              <span>Arquitectura multi-supermercado</span>
            </div>
          </div>

          <aside className={styles.heroPanel} aria-label="Resumen del producto">
            <span className={styles.panelLabel}>Vista previa del objetivo</span>
            <div className={styles.weekHeader}>
              <div>
                <small>Semana equilibrada</small>
                <strong>7 días · 4 comidas</strong>
              </div>
              <span>Disponible con cuenta</span>
            </div>
            <div className={styles.macroGrid}>
              <div>
                <span>2.200</span>
                <small>kcal / día</small>
              </div>
              <div>
                <span>145 g</span>
                <small>proteína / día</small>
              </div>
            </div>
            <div className={styles.budgetRow}>
              <span>Presupuesto objetivo</span>
              <strong>65,00 €</strong>
            </div>
            <div className={styles.progressTrack}>
              <span />
            </div>
            <p>
              Explora el catálogo sin registrarte. Inicia sesión para personalizar y guardar tus
              planes.
            </p>
          </aside>
        </section>

        <section className={styles.notice} aria-label="Aviso sobre los datos">
          <span className={styles.noticeIcon} aria-hidden="true">
            i
          </span>
          <div>
            <strong>Datos de demostración</strong>
            <p>
              Los productos, precios y valores nutricionales actuales son ficticios y permiten
              validar el funcionamiento técnico de extremo a extremo.
            </p>
          </div>
        </section>

        <section className={styles.story} aria-labelledby="problem-heading">
          <div>
            <span>El problema</span>
            <h2 id="problem-heading">Un menú no termina cuando eliges las recetas</h2>
          </div>
          <p>
            Comprar paquetes completos cambia el coste y deja sobrantes. Aquí nutrición, presupuesto
            y aprovechamiento se evalúan juntos y con datos rastreables.
          </p>
        </section>

        <section className={styles.selectorSection} id="supermercados">
          {supermarkets.isPending ? (
            <div className={styles.loading} aria-live="polite">
              Cargando supermercados…
            </div>
          ) : supermarkets.isError ? (
            <div className={styles.loadError} role="alert">
              No se pudo cargar la lista de supermercados. Asegúrate de que el backend está
              disponible.
            </div>
          ) : (
            <SupermarketSelector
              supermarkets={supermarkets.data}
              selectedCode={activeSupermarketCode}
              onSelect={setSelectedCode}
            />
          )}
        </section>

        <section
          className={styles.workflow}
          id="como-funcionara"
          aria-labelledby="workflow-heading"
        >
          <div className={styles.sectionHeading}>
            <span>Cómo funciona</span>
            <h2 id="workflow-heading">De tus objetivos a una compra útil</h2>
            <p>
              El núcleo es determinista: cada cifra puede rastrearse hasta un producto, una cantidad
              y un paquete.
            </p>
          </div>
          <div className={styles.workflowGrid}>
            {workflowSteps.map((step) => (
              <article key={step.number}>
                <span>{step.number}</span>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </article>
            ))}
          </div>
        </section>

        <ProductPreview supermarketCode={activeSupermarketCode} />

        <section className={styles.benefits} aria-labelledby="benefits-heading">
          <div className={styles.sectionHeading}>
            <span>Beneficios</span>
            <h2 id="benefits-heading">Decisiones claras, sin cifras decorativas</h2>
          </div>
          <div className={styles.workflowGrid}>
            <article>
              <h3>Compra real</h3>
              <p>Compara coste consumido, paquetes necesarios y coste de sobrantes.</p>
            </article>
            <article>
              <h3>Edición controlada</h3>
              <p>Previsualiza sustituciones antes de cambiar un plan guardado.</p>
            </article>
            <article>
              <h3>Datos privados</h3>
              <p>Tus planes y listas pertenecen a tu cuenta; el catálogo sigue siendo público.</p>
            </article>
          </div>
        </section>

        <section className={styles.technical}>
          <div>
            <span>Enfoque técnico</span>
            <h2>Cálculos deterministas y snapshots históricos</h2>
            <p>
              Las copias conservan cantidades y precios originales. Cada plan puede explicarse sin
              consultar de nuevo el catálogo ni depender de una respuesta opaca.
            </p>
          </div>
          <div className={styles.cta}>
            <h2>Empieza con el catálogo o crea tu espacio privado</h2>
            <div>
              <Link to="/register">Crear cuenta</Link>
              <Link to="/products">Ver catálogo</Link>
            </div>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        <div className={styles.footerBrand}>
          <span className={styles.brandMark} aria-hidden="true">
            S
          </span>
          <div>
            <strong>Supermarket Meal Planner</strong>
            <small>Proyecto independiente · Planificación privada y catálogo público</small>
          </div>
        </div>
        <div className={styles.legal}>
          <p>
            Supermarket Meal Planner es un proyecto independiente y no está afiliado, patrocinado ni
            respaldado por los supermercados mostrados en la plataforma. Los nombres y marcas
            pertenecen a sus respectivos propietarios.
          </p>
          <p>
            El precio y la disponibilidad pueden variar según la tienda, la ubicación y el momento
            de la consulta.
          </p>
        </div>
      </footer>
    </div>
  )
}
