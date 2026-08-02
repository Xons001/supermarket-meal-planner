import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type HTMLAttributes,
  type ReactNode,
} from 'react'
import styles from './Ui.module.css'

export function Button({
  variant = 'primary',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger'
}) {
  return <button className={styles.button} data-variant={variant} {...props} />
}

export function Card(props: HTMLAttributes<HTMLElement>) {
  return <article {...props} className={`${styles.card} ${props.className ?? ''}`} />
}

export function Badge({
  tone = 'neutral',
  ...props
}: HTMLAttributes<HTMLSpanElement> & { tone?: 'neutral' | 'success' | 'warning' }) {
  return <span {...props} className={styles.badge} data-tone={tone} />
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <section className={styles.empty}>
      <h2>{title}</h2>
      {children}
    </section>
  )
}

export function Skeleton({ height = '1rem' }: { height?: string }) {
  return <div className={styles.skeleton} data-height={height} aria-hidden="true" />
}

export function FilterBar({ children }: { children: ReactNode }) {
  return <section className={styles.filterBar}>{children}</section>
}

export function Pagination({
  page,
  totalPages,
  onPage,
}: {
  page: number
  totalPages: number
  onPage: (page: number) => void
}) {
  return (
    <nav className={styles.pagination} aria-label="Paginación">
      <Button variant="secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>
        Anterior
      </Button>
      <span>
        Página {page + 1} de {Math.max(1, totalPages)}
      </span>
      <Button
        variant="secondary"
        disabled={page + 1 >= totalPages}
        onClick={() => onPage(page + 1)}
      >
        Siguiente
      </Button>
    </nav>
  )
}

export function Modal({
  open,
  title,
  children,
  onClose,
}: {
  open: boolean
  title: string
  children: ReactNode
  onClose: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    if (open && !ref.current?.open) ref.current?.showModal()
    if (!open && ref.current?.open) ref.current.close()
  }, [open])
  return (
    <dialog ref={ref} className={styles.dialog} onClose={onClose}>
      <div className={styles.dialogBody}>
        <h2>{title}</h2>
        {children}
        <div className={styles.dialogActions}>
          <Button variant="secondary" onClick={onClose}>
            Cerrar
          </Button>
        </div>
      </div>
    </dialog>
  )
}

interface Toast {
  id: number
  message: string
  tone: 'info' | 'error'
}
const ToastContext = createContext<(message: string, tone?: Toast['tone']) => void>(() => undefined)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const notify = (message: string, tone: Toast['tone'] = 'info') => {
    const id = Date.now() + Math.random()
    setToasts((values) => [...values, { id, message, tone }])
    window.setTimeout(
      () => setToasts((values) => values.filter((toast) => toast.id !== id)),
      tone === 'error' ? 10000 : 4500,
    )
  }
  return (
    <ToastContext.Provider value={notify}>
      {children}
      <aside className={styles.toasts} aria-live="polite" aria-atomic="false">
        {toasts.map((toast) => (
          <div key={toast.id} className={styles.toast} data-tone={toast.tone} role="status">
            <span>{toast.message}</span>
            <button
              aria-label="Cerrar notificación"
              onClick={() => setToasts((values) => values.filter((value) => value.id !== toast.id))}
            >
              ×
            </button>
          </div>
        ))}
      </aside>
    </ToastContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useToast = () => useContext(ToastContext)
