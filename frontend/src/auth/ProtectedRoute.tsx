import { Navigate, useLocation } from 'react-router'
import type { ReactNode } from 'react'
import { useAuth } from './AuthProvider'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading)
    return (
      <main>
        <p role="status">Restaurando sesión…</p>
      </main>
    )
  if (!user) {
    const destination = `${location.pathname}${location.search}`
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: destination.startsWith('/') ? destination : '/' }}
      />
    )
  }
  return children
}

export function AdminRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading)
    return (
      <main>
        <p role="status">Restaurando sesión…</p>
      </main>
    )
  if (user?.role !== 'ADMIN') return <Navigate to="/forbidden" replace />
  return children
}
