import { render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { expect, test, vi } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'

function Broken(): ReactElement {
  throw new Error('sensitive implementation detail')
}

test('replaces an unexpected render failure with a safe recovery page', () => {
  vi.spyOn(console, 'error').mockImplementation(() => undefined)
  render(
    <ErrorBoundary>
      <Broken />
    </ErrorBoundary>,
  )
  expect(screen.getByRole('heading', { name: 'No pudimos mostrar esta página' })).toBeVisible()
  expect(screen.queryByText('sensitive implementation detail')).not.toBeInTheDocument()
  vi.restoreAllMocks()
})
