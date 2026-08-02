import { useEffect } from 'react'
import { useLocation } from 'react-router'

const privatePrefixes = [
  '/dashboard',
  '/profile',
  '/meal-plans',
  '/shopping-lists',
  '/activity',
  '/admin',
]

export function RouteMetadata() {
  const { pathname } = useLocation()
  useEffect(() => {
    const privatePage = privatePrefixes.some((prefix) => pathname.startsWith(prefix))
    setMeta('robots', privatePage ? 'noindex, nofollow' : 'index, follow')
    const configured = import.meta.env.VITE_PUBLIC_BASE_URL
    const base = configured?.replace(/\/$/, '') ?? window.location.origin
    let canonical = document.querySelector<HTMLLinkElement>('link[rel="canonical"]')
    if (!canonical) {
      canonical = document.createElement('link')
      canonical.rel = 'canonical'
      document.head.append(canonical)
    }
    canonical.href = `${base}${pathname}`
  }, [pathname])
  return null
}

function setMeta(name: string, content: string) {
  let element = document.querySelector<HTMLMetaElement>(`meta[name="${name}"]`)
  if (!element) {
    element = document.createElement('meta')
    element.name = name
    document.head.append(element)
  }
  element.content = content
}
