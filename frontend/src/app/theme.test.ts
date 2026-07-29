import { beforeEach, describe, expect, it, vi } from 'vitest'
import { applyTheme, saveVisitorTheme, visitorTheme } from './theme'

describe('theme', () => {
  beforeEach(() => {
    localStorage.clear()
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn().mockReturnValue({ matches: true }),
    })
  })

  it('uses SYSTEM by default and resolves the operating system before React', () => {
    expect(visitorTheme()).toBe('SYSTEM')
    applyTheme('SYSTEM')
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('stores only the visitor preference', () => {
    saveVisitorTheme('LIGHT')
    expect(visitorTheme()).toBe('LIGHT')
    expect(document.documentElement.dataset.theme).toBe('light')
  })
})
