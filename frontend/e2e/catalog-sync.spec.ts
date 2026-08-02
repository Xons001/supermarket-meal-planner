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
  await page.getByLabel('Correo').fill(process.env.E2E_ADMIN_EMAIL!)
  await page.getByLabel('Contraseña').fill(process.env.E2E_ADMIN_PASSWORD!)
  await page.unroute('**/api/v1/auth/me')
  await page.unroute('**/api/v1/auth/refresh')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)

  await page.goto('/admin/catalog-sync')
  await expect(page.getByRole('heading', { name: /Sincronización del catálogo/ })).toBeVisible()
  await expect(page.getByText('LOCAL_JSON')).toBeVisible()
  await expect(page.locator('body')).not.toHaveCSS('overflow-x', 'scroll')
})
