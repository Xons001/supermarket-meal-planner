import { Link, NavLink } from 'react-router-dom'
import { BackendStatus } from './BackendStatus'
import styles from './SiteHeader.module.css'

export function SiteHeader() {
  return (
    <header className={styles.header}>
      <Link className={styles.brand} to="/" aria-label="Supermarket Meal Planner, inicio">
        <span className={styles.brandMark} aria-hidden="true">
          S
        </span>
        <span>
          Supermarket
          <small>Meal Planner</small>
        </span>
      </Link>
      <nav className={styles.nav} aria-label="Navegación principal">
        <NavLink to="/" end>
          Inicio
        </NavLink>
        <NavLink to="/products">Catálogo</NavLink>
        <NavLink to="/meal-templates">Plantillas</NavLink>
        <NavLink to="/meal-plans">Planes</NavLink>
      </nav>
      <BackendStatus />
    </header>
  )
}
