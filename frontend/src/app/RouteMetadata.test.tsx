import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { expect, test } from 'vitest'
import { RouteMetadata } from './RouteMetadata'

test('marks private pages as noindex', () => {
  render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <RouteMetadata />
    </MemoryRouter>,
  )
  expect(document.querySelector('meta[name="robots"]')).toHaveAttribute(
    'content',
    'noindex, nofollow',
  )
})
