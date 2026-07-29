import { createContext, useCallback, useContext, useEffect, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getMe } from '../api/auth'
import { ApiError } from '../api/client'
import type { AuthUser } from '../types/auth'
import { applyTheme, visitorTheme } from '../app/theme'

interface AuthContextValue {
  user: AuthUser | null
  loading: boolean
  setIdentity: (user: AuthUser | null) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({
  children,
  initialUser,
}: {
  children: ReactNode
  initialUser?: AuthUser | null
}) {
  const client = useQueryClient()
  const query = useQuery<AuthUser | null>({
    queryKey: ['auth', 'me'],
    queryFn: getMe,
    initialData: initialUser,
    enabled: initialUser === undefined,
    retry: (count, error) => !(error instanceof ApiError && error.status === 401) && count < 1,
  })
  const setIdentity = useCallback(
    (user: AuthUser | null) => {
      client.removeQueries({
        predicate: (query) => query.queryKey[0] !== 'auth',
      })
      client.getMutationCache().clear()
      client.setQueryData(['auth', 'me'], user)
    },
    [client],
  )
  useEffect(() => {
    const expired = () => setIdentity(null)
    window.addEventListener('smp:session-expired', expired)
    return () => window.removeEventListener('smp:session-expired', expired)
  }, [setIdentity])
  useEffect(() => {
    applyTheme(query.data?.preferences.theme ?? visitorTheme())
    const media =
      typeof window.matchMedia === 'function'
        ? window.matchMedia('(prefers-color-scheme: dark)')
        : null
    const refreshSystem = () => {
      const selected = query.data?.preferences.theme ?? visitorTheme()
      if (selected === 'SYSTEM') applyTheme(selected)
    }
    media?.addEventListener('change', refreshSystem)
    return () => media?.removeEventListener('change', refreshSystem)
  }, [query.data?.id, query.data?.preferences.theme])
  return (
    <AuthContext.Provider
      value={{ user: query.data ?? null, loading: query.isLoading, setIdentity }}
    >
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  return value ?? { user: null, loading: false, setIdentity: () => undefined }
}
