import { Component, type ErrorInfo, type ReactNode } from 'react'
import styles from '../pages/StatePage.module.css'

interface State {
  failed: boolean
}

export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('unexpected_ui_error', { name: error.name, componentStack: info.componentStack })
  }

  render() {
    if (!this.state.failed) return this.props.children
    return (
      <main className={styles.page}>
        <span>Error inesperado</span>
        <h1>No pudimos mostrar esta página</h1>
        <p>Recarga la aplicación. Tus datos guardados no se han modificado.</p>
        <button onClick={() => window.location.reload()}>Volver a cargar</button>
        <a href="/">Ir al inicio</a>
      </main>
    )
  }
}
