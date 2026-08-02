import type { ThemePreference } from '../types/auth'

const VISITOR_THEME_KEY = 'smp-visitor-theme'

export function visitorTheme(): ThemePreference {
  const value = localStorage.getItem(VISITOR_THEME_KEY)
  return value === 'LIGHT' || value === 'DARK' || value === 'SYSTEM' ? value : 'SYSTEM'
}

export function saveVisitorTheme(theme: ThemePreference) {
  localStorage.setItem(VISITOR_THEME_KEY, theme)
  applyTheme(theme)
}

export function applyTheme(theme: ThemePreference) {
  const dark =
    theme === 'DARK' ||
    (theme === 'SYSTEM' &&
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-color-scheme: dark)').matches)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
}
