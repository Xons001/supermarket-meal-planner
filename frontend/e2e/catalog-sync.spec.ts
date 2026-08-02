import { expect, test } from '@playwright/test'

test('an isolated administrator can open the catalog synchronization panel', async ({ page }) => {
  const anonymousProblem = JSON.stringify({
    status: 401,
    code: 'AUTHENTICATION_REQUIRED',
    detail: 'Anonymous E2E fixture',
  })
  await page.route('**/api/v1/auth/me', (route) =>
    route.fulfill({ status: 401, contentType: 'application/problem+json', body: anonymousProblem }),
  )
  await page.route('**/api/v1/auth/refresh', (route) =>
    route.fulfill({ status: 401, contentType: 'application/problem+json', body: anonymousProblem }),
  )
  await page.goto('/login')

  const login = await page.evaluate(
    async ({ email, password }) => {
      const csrfResponse = await fetch('/api/v1/auth/csrf', { credentials: 'include' })
      const csrf = document.cookie
        .split('; ')
        .find((cookie) => cookie.startsWith('XSRF-TOKEN='))
        ?.split('=')[1]
      if (!csrf || !csrfResponse.ok) {
        return { status: csrfResponse.status, body: 'CSRF unavailable' }
      }
      const response = await fetch('/api/v1/auth/login', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': decodeURIComponent(csrf),
        },
        body: JSON.stringify({ email, password }),
      })
      return { status: response.status, body: await response.text() }
    },
    { email: process.env.E2E_ADMIN_EMAIL!, password: process.env.E2E_ADMIN_PASSWORD! },
  )
  expect(login.status, login.body).toBe(200)

  await page.unroute('**/api/v1/auth/me')
  await page.unroute('**/api/v1/auth/refresh')
  await page.goto('/admin/catalog-sync')
  await expect(page.getByRole('heading', { name: 'Sincronización del catálogo' })).toBeVisible()
  await expect(page.getByText('LOCAL_JSON')).toBeVisible()
  await expect(page.locator('body')).not.toHaveCSS('overflow-x', 'scroll')
})
