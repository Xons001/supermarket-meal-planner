import { useState } from 'react'
import { Link, NavLink } from 'react-router'
import { logout, updatePreferences } from '../api/auth'
import { saveVisitorTheme, visitorTheme } from '../app/theme'
import { useAuth } from '../auth/AuthProvider'
import type { ThemePreference } from '../types/auth'
import { BackendStatus } from './BackendStatus'
import { useToast } from './ui'
import styles from './SiteHeader.module.css'

export function SiteHeader() {
  const { user, loading, setIdentity } = useAuth()
  const notify = useToast()
  const [loggingOut, setLoggingOut] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  const logoutCurrentUser = async () => {
    setLoggingOut(true)
    try {
      await logout()
      notify('Sesión cerrada')
    } finally {
      setIdentity(null)
      setLoggingOut(false)
      setMobileOpen(false)
    }
  }

  const setTheme = async (theme: ThemePreference) => {
    if (!user) {
      saveVisitorTheme(theme)
      return
    }
    const preferences = await updatePreferences({ ...user.preferences, theme })
    setIdentity({ ...user, preferences })
    notify('Tema actualizado')
  }

  const links: Array<[string, string]> = user
    ? [
        ['/dashboard', 'Dashboard'],
        ['/meal-plans', 'Mis planes'],
        ['/shopping-lists', 'Mis listas'],
        ['/products', 'Catálogo'],
        ['/activity', 'Actividad'],
      ]
    : [
        ['/', 'Inicio'],
        ['/products', 'Catálogo'],
        ['/#como-funcionara', 'Cómo funciona'],
      ]

  if (user?.role === 'ADMIN') links.push(['/admin/catalog-sync', 'Sincronización'])

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
        {links.map(([to, label]) =>
          to.includes('#') ? (
            <Link key={to} to={to}>
              {label}
            </Link>
          ) : (
            <NavLink key={to} to={to} end={to === '/' || to === '/dashboard'}>
              {label}
            </NavLink>
          ),
        )}
      </nav>

      <div className={styles.actions}>
        <BackendStatus />
        {loading ? (
          <span className={styles.accountLoading}>Cuenta</span>
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
          <details className={styles.accountMenu}>
            <summary aria-label="Abrir menú de cuenta">
              <span aria-hidden="true">{user.displayName.trim().charAt(0).toUpperCase()}</span>
              <strong>{user.displayName}</strong>
            </summary>
            <div>
              <Link to="/profile">Perfil</Link>
              <Link to="/profile#preferences">Preferencias</Link>
              <label>
                Tema
                <select
                  value={user.preferences.theme ?? 'SYSTEM'}
                  onChange={(event) => void setTheme(event.target.value as ThemePreference)}
                >
                  <option value="SYSTEM">Sistema</option>
                  <option value="LIGHT">Claro</option>
                  <option value="DARK">Oscuro</option>
                </select>
              </label>
              <button disabled={loggingOut} onClick={() => void logoutCurrentUser()}>
                {loggingOut ? 'Cerrando…' : 'Cerrar sesión'}
              </button>
            </div>
          </details>
        )}
        <button
          className={styles.mobileToggle}
          aria-expanded={mobileOpen}
          aria-controls="mobile-navigation"
          aria-label="Abrir menú"
          onClick={() => setMobileOpen((value) => !value)}
        >
          ☰
        </button>
      </div>

      {mobileOpen && (
        <div className={styles.drawerBackdrop} onClick={() => setMobileOpen(false)}>
          <aside
            id="mobile-navigation"
            className={styles.drawer}
            aria-label="Menú móvil"
            onClick={(event) => event.stopPropagation()}
          >
            <button aria-label="Cerrar menú" onClick={() => setMobileOpen(false)}>
              ×
            </button>
            <nav>
              {links.map(([to, label]) => (
                <Link key={to} to={to} onClick={() => setMobileOpen(false)}>
                  {label}
                </Link>
              ))}
              {!user && <Link to="/login">Iniciar sesión</Link>}
              {!user && <Link to="/register">Crear cuenta</Link>}
              {user && <Link to="/profile">Perfil y preferencias</Link>}
            </nav>
            {!user && (
              <label>
                Tema
                <select
                  defaultValue={visitorTheme()}
                  onChange={(event) => void setTheme(event.target.value as ThemePreference)}
                >
                  <option value="SYSTEM">Sistema</option>
                  <option value="LIGHT">Claro</option>
                  <option value="DARK">Oscuro</option>
                </select>
              </label>
            )}
            {user && (
              <button disabled={loggingOut} onClick={() => void logoutCurrentUser()}>
                Cerrar sesión
              </button>
            )}
          </aside>
        </div>
      )}
    </header>
  )
}
