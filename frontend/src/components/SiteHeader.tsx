import { useState } from 'react'
import { Link, NavLink } from 'react-router'
import { logout } from '../api/auth'
import { useAuth } from '../auth/AuthProvider'
import { BackendStatus } from './BackendStatus'
import styles from './SiteHeader.module.css'

export function SiteHeader() {
  const { user, loading, setIdentity } = useAuth()
  const [loggingOut, setLoggingOut] = useState(false)

  const logoutCurrentUser = async () => {
    setLoggingOut(true)
    try {
      await logout()
    } finally {
      setIdentity(null)
      setLoggingOut(false)
    }
  }

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
        <NavLink to="/meal-plans/new">Planificar</NavLink>
        {user && <NavLink to="/meal-plans">Mis planes</NavLink>}
        {user && <NavLink to="/shopping-lists">Mis listas</NavLink>}
      </nav>

      <div className={styles.actions}>
        <BackendStatus />
        {loading ? (
          <span className={styles.accountLoading} aria-label="Comprobando sesión">
            Cuenta
          </span>
        ) : !user ? (
          <div className={styles.authActions}>
            <NavLink className={styles.loginLink} to="/login">
              Iniciar sesión
            </NavLink>
            <NavLink className={styles.registerLink} to="/register">
              Crear cuenta
            </NavLink>
          </div>
        ) : (
          <div className={styles.userActions}>
            <NavLink className={styles.profileLink} to="/profile" aria-label="Abrir mi perfil">
              <span aria-hidden="true">{user.displayName.trim().charAt(0).toUpperCase()}</span>
              <span>
                <small>Mi perfil</small>
                <strong>{user.displayName}</strong>
              </span>
            </NavLink>
            <button
              className={styles.logoutButton}
              type="button"
              disabled={loggingOut}
              onClick={() => void logoutCurrentUser()}
              aria-label={loggingOut ? 'Cerrando sesión' : 'Cerrar sesión'}
              title="Cerrar sesión"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M10 5H6.8A1.8 1.8 0 0 0 5 6.8v10.4A1.8 1.8 0 0 0 6.8 19H10M14.5 8.5 18 12l-3.5 3.5M9 12h9" />
              </svg>
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
