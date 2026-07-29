import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { login, register } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthProvider'
import { SiteHeader } from '../components/SiteHeader'
import styles from './IdentityPages.module.css'

const schema = z.object({
  displayName: z.string().trim().max(120).optional(),
  email: z.email('Introduce un correo válido'),
  password: z.string().min(10, 'La contraseña debe tener al menos 10 caracteres').max(128),
})
type Fields = z.infer<typeof schema>

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { user, setIdentity } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const {
    register: field,
    handleSubmit,
    formState,
  } = useForm<Fields>({
    resolver: zodResolver(schema),
    defaultValues: { displayName: '', email: '', password: '' },
  })
  if (user) return <Navigate to="/meal-plans" replace />
  const submit = handleSubmit(async (values) => {
    setError('')
    try {
      const authenticated =
        mode === 'register'
          ? await register({
              email: values.email,
              password: values.password,
              displayName: values.displayName?.trim() || '',
            })
          : await login({ email: values.email, password: values.password })
      setIdentity(authenticated)
      const state = location.state as { from?: unknown } | null
      const destination =
        typeof state?.from === 'string' &&
        state.from.startsWith('/') &&
        !state.from.startsWith('//')
          ? state.from
          : '/meal-plans'
      navigate(destination, { replace: true })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No se pudo completar la operación')
    }
  })
  return (
    <>
      <SiteHeader />
      <main className={styles.page}>
        <form className={styles.card} onSubmit={submit}>
          <p className={styles.eyebrow}>Tu espacio privado</p>
          <h1>{mode === 'login' ? 'Inicia sesión' : 'Crea tu cuenta'}</h1>
          <p>
            {mode === 'login'
              ? 'Accede a tus planes y listas de compra.'
              : 'Guarda planes, preferencias y ediciones de forma segura.'}
          </p>
          {mode === 'register' && (
            <label>
              Nombre
              <input autoComplete="name" {...field('displayName')} required />
              {formState.errors.displayName && (
                <small>{formState.errors.displayName.message}</small>
              )}
            </label>
          )}
          <label>
            Correo
            <input type="email" autoComplete="email" {...field('email')} />
            {formState.errors.email && <small>{formState.errors.email.message}</small>}
          </label>
          <label>
            Contraseña
            <input
              type="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              {...field('password')}
            />
            {formState.errors.password && <small>{formState.errors.password.message}</small>}
          </label>
          {error && (
            <p role="alert" className={styles.error}>
              {error}
            </p>
          )}
          <button disabled={formState.isSubmitting}>
            {formState.isSubmitting ? 'Procesando…' : mode === 'login' ? 'Entrar' : 'Registrarme'}
          </button>
          <p>
            {mode === 'login' ? '¿Aún no tienes cuenta? ' : '¿Ya tienes cuenta? '}
            <Link to={mode === 'login' ? '/register' : '/login'}>
              {mode === 'login' ? 'Regístrate' : 'Inicia sesión'}
            </Link>
          </p>
        </form>
      </main>
    </>
  )
}
