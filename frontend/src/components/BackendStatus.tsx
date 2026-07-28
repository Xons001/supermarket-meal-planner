import { useBackendHealth } from '../hooks/useCatalogQueries'
import styles from './BackendStatus.module.css'

export function BackendStatus() {
  const health = useBackendHealth()

  if (health.isPending) {
    return (
      <span className={styles.status} aria-live="polite">
        <span className={`${styles.dot} ${styles.checking}`} />
        Comprobando conexión
      </span>
    )
  }

  if (health.isError || health.data.status !== 'UP') {
    return (
      <span className={styles.status} aria-live="polite">
        <span className={`${styles.dot} ${styles.offline}`} />
        Backend no disponible
      </span>
    )
  }

  return (
    <span className={styles.status} aria-live="polite">
      <span className={`${styles.dot} ${styles.online}`} />
      Backend conectado
    </span>
  )
}
