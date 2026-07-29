import { Link } from 'react-router'
import { SiteHeader } from '../components/SiteHeader'
import styles from './StatePage.module.css'

export function StatePage({
  code,
  title,
  description,
}: {
  code: string
  title: string
  description: string
}) {
  return (
    <>
      <SiteHeader />
      <main className={styles.page}>
        <span>{code}</span>
        <h1>{title}</h1>
        <p>{description}</p>
        <Link to="/">Volver al inicio</Link>
      </main>
    </>
  )
}
