import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import {
  supermarketSelectionSchema,
  type SupermarketSelection,
} from '../../schemas/supermarketSelection'
import type { Supermarket } from '../../types/api'
import styles from './SupermarketSelector.module.css'

interface SupermarketSelectorProps {
  supermarkets: Supermarket[]
  selectedCode: string | undefined
  onSelect: (supermarketCode: string) => void
}

export function SupermarketSelector({
  supermarkets,
  selectedCode,
  onSelect,
}: SupermarketSelectorProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<SupermarketSelection>({
    resolver: zodResolver(supermarketSelectionSchema),
    defaultValues: {
      supermarketCode: selectedCode ?? '',
    },
  })

  useEffect(() => {
    reset({ supermarketCode: selectedCode ?? '' })
  }, [reset, selectedCode])

  const submitSelection = (selection: SupermarketSelection) => {
    onSelect(selection.supermarketCode)
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit(submitSelection)}>
      <fieldset className={styles.fieldset}>
        <legend className={styles.legend}>Selecciona tu supermercado</legend>
        <p className={styles.help}>
          El catálogo se obtiene mediante un proveedor intercambiable. En esta fase solo hay un
          catálogo de demostración.
        </p>

        <div className={styles.grid}>
          {supermarkets.map((supermarket) => (
            <label
              className={`${styles.card} ${
                selectedCode === supermarket.code ? styles.selected : ''
              } ${!supermarket.enabled ? styles.disabled : ''}`}
              key={supermarket.code}
            >
              <input
                className={styles.radio}
                type="radio"
                value={supermarket.code}
                disabled={!supermarket.enabled}
                {...register('supermarketCode')}
              />
              <span className={styles.monogram} aria-hidden="true">
                {supermarket.name.slice(0, 1)}
              </span>
              <span className={styles.cardBody}>
                <strong>{supermarket.name}</strong>
                <small>{supermarket.enabled ? 'Disponible' : 'Próximamente'}</small>
              </span>
              <span
                className={`${styles.availability} ${
                  supermarket.enabled ? styles.available : styles.comingSoon
                }`}
              >
                {supermarket.enabled ? 'Activo' : 'Próximamente'}
              </span>
            </label>
          ))}
        </div>
      </fieldset>

      {errors.supermarketCode ? (
        <p className={styles.error} role="alert">
          {errors.supermarketCode.message}
        </p>
      ) : null}

      <button className={styles.button} type="submit">
        Ver catálogo de demostración
      </button>
    </form>
  )
}
