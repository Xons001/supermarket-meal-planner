import { SiteHeader } from '../components/SiteHeader'
import styles from './IdentityPages.module.css'

export function LegalPage({ kind }: { kind: 'privacy' | 'terms' }) {
  const privacy = kind === 'privacy'
  return (
    <>
      <SiteHeader />
      <main className={styles.profile}>
        <article className={styles.card}>
          <p className={styles.eyebrow}>Información de demostración</p>
          <h1>{privacy ? 'Privacidad' : 'Términos de uso'}</h1>
          {privacy ? (
            <>
              <p>
                Guardamos los datos necesarios para tu cuenta, preferencias, planes, listas y
                actividad.
              </p>
              <p>
                Puedes descargar una copia JSON desde tu perfil. Desactivar la cuenta conserva los
                recursos para mantener su integridad histórica.
              </p>
              <p>
                No incorporamos analítica publicitaria. Los registros técnicos evitan contraseñas,
                tokens y correos completos.
              </p>
            </>
          ) : (
            <>
              <p>
                Esta aplicación es una demostración técnica independiente y no constituye
                asesoramiento médico, nutricional ni jurídico.
              </p>
              <p>
                Precios, disponibilidad y nutrición pueden estar incompletos o cambiar. Verifica
                siempre etiquetas y recomendaciones profesionales.
              </p>
              <p>
                Los nombres comerciales pertenecen a sus propietarios; no existe afiliación ni
                patrocinio.
              </p>
            </>
          )}
        </article>
      </main>
    </>
  )
}
