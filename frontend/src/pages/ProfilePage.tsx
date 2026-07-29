import { useState } from 'react'
import { changePassword, disableAccount, updatePreferences, updateProfile } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthProvider'
import { SiteHeader } from '../components/SiteHeader'
import type { GenerationStrategy, OptimizationPreset } from '../types/auth'
import styles from './IdentityPages.module.css'

export function ProfilePage() {
  const { user, setIdentity } = useAuth()
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  if (!user) return null
  const run = async (operation: () => Promise<void>) => {
    setError('')
    setMessage('')
    try {
      await operation()
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No se pudo guardar')
    }
  }
  return (
    <>
      <SiteHeader />
      <main className={styles.profile}>
        <header>
          <p className={styles.eyebrow}>Cuenta</p>
          <h1>Perfil y preferencias</h1>
          <p>{user.email}</p>
        </header>
        {message && (
          <p role="status" className={styles.success}>
            {message}
          </p>
        )}
        {error && (
          <p role="alert" className={styles.error}>
            {error}
          </p>
        )}
        <form
          className={styles.card}
          onSubmit={(event) => {
            event.preventDefault()
            const data = new FormData(event.currentTarget)
            void run(async () => {
              const next = await updateProfile({ displayName: String(data.get('displayName')) })
              setIdentity(next)
              setMessage('Perfil actualizado')
            })
          }}
        >
          <h2>Datos personales</h2>
          <label>
            Nombre
            <input name="displayName" defaultValue={user.displayName} required maxLength={120} />
          </label>
          <button>Guardar nombre</button>
        </form>
        <form
          className={styles.card}
          onSubmit={(event) => {
            event.preventDefault()
            const data = new FormData(event.currentTarget)
            void run(async () => {
              const strategy = String(data.get('strategy')) as GenerationStrategy
              const next = await updatePreferences({
                dailyCaloriesTarget: Number(data.get('calories')),
                dailyProteinTarget: Number(data.get('protein')),
                weeklyBudget: Number(data.get('budget')),
                numberOfDays: Number(data.get('days')),
                mealsPerDay: Number(data.get('meals')),
                strategy,
                optimizationPreset:
                  strategy === 'SCORING'
                    ? null
                    : (String(data.get('preset')) as OptimizationPreset),
                dietaryRestrictions: split(String(data.get('dietary'))),
                allergens: split(String(data.get('allergens'))),
                theme: String(data.get('theme')) as import('../types/auth').ThemePreference,
              })
              setIdentity({ ...user, preferences: next })
              setMessage('Preferencias guardadas')
            })
          }}
        >
          <h2>Preferencias del generador</h2>
          <div className={styles.grid}>
            <label>
              Calorías diarias
              <input
                name="calories"
                type="number"
                min="1"
                defaultValue={user.preferences.dailyCaloriesTarget}
              />
            </label>
            <label>
              Proteína diaria (g)
              <input
                name="protein"
                type="number"
                min="1"
                defaultValue={user.preferences.dailyProteinTarget}
              />
            </label>
            <label>
              Presupuesto semanal (€)
              <input
                name="budget"
                type="number"
                min="0"
                step="0.01"
                defaultValue={user.preferences.weeklyBudget ?? 70}
              />
            </label>
            <label>
              Días
              <input
                name="days"
                type="number"
                min="1"
                max="7"
                defaultValue={user.preferences.numberOfDays}
              />
            </label>
            <label>
              Comidas por día
              <input
                name="meals"
                type="number"
                min="1"
                max="6"
                defaultValue={user.preferences.mealsPerDay}
              />
            </label>
            <label>
              Estrategia
              <select name="strategy" defaultValue={user.preferences.strategy}>
                <option value="PURCHASE_AWARE_SCORING">Compra eficiente</option>
                <option value="SCORING">Clásico</option>
              </select>
            </label>
            <label>
              Prioridad
              <select
                name="preset"
                defaultValue={user.preferences.optimizationPreset ?? 'BALANCED'}
              >
                <option value="BALANCED">Equilibrada</option>
                <option value="LOWER_PURCHASE_COST">Menor compra</option>
                <option value="LOWER_WASTE">Menos desperdicio</option>
                <option value="MORE_REUSE">Más aprovechamiento</option>
              </select>
            </label>
            <label>
              Tema
              <select name="theme" defaultValue={user.preferences.theme ?? 'SYSTEM'}>
                <option value="SYSTEM">Usar el sistema</option>
                <option value="LIGHT">Claro</option>
                <option value="DARK">Oscuro</option>
              </select>
            </label>
          </div>
          <label>
            Restricciones, separadas por comas
            <input name="dietary" defaultValue={user.preferences.dietaryRestrictions.join(', ')} />
          </label>
          <label>
            Alérgenos excluidos, separados por comas
            <input name="allergens" defaultValue={user.preferences.allergens.join(', ')} />
          </label>
          <button>Guardar preferencias</button>
        </form>
        <form
          className={styles.card}
          onSubmit={(event) => {
            event.preventDefault()
            const form = event.currentTarget
            const data = new FormData(form)
            void run(async () => {
              await changePassword({
                currentPassword: String(data.get('current')),
                newPassword: String(data.get('next')),
              })
              setIdentity(null)
              form.reset()
            })
          }}
        >
          <h2>Cambiar contraseña</h2>
          <label>
            Contraseña actual
            <input name="current" type="password" autoComplete="current-password" required />
          </label>
          <label>
            Nueva contraseña
            <input
              name="next"
              type="password"
              autoComplete="new-password"
              minLength={10}
              maxLength={128}
              required
            />
          </label>
          <button>Cambiar y cerrar todas las sesiones</button>
        </form>
        <form
          className={`${styles.card} ${styles.danger}`}
          onSubmit={(event) => {
            event.preventDefault()
            const data = new FormData(event.currentTarget)
            if (!window.confirm('¿Desactivar la cuenta? Tus datos se conservarán.')) return
            void run(async () => {
              await disableAccount(String(data.get('password')))
              setIdentity(null)
            })
          }}
        >
          <h2>Desactivar cuenta</h2>
          <label>
            Confirma tu contraseña
            <input name="password" type="password" required />
          </label>
          <button>Desactivar mi cuenta</button>
        </form>
      </main>
    </>
  )
}

function split(value: string) {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}
