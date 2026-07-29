import { expect, test } from '@playwright/test'

test('public navigation, isolated registration and private dashboard fit the viewport', async ({
  page,
}, testInfo) => {
  const email = `e2e-${testInfo.project.name}-${Date.now()}@example.test`
  await page.goto('/')
  const publicMenu = page.getByRole('button', { name: 'Abrir menú' })
  if (await publicMenu.isVisible()) {
    await publicMenu.click()
  }
  await expect(page.getByRole('link', { name: 'Iniciar sesión' }).first()).toBeVisible()
  await expect(page.getByRole('link', { name: 'Crear cuenta' }).first()).toBeVisible()
  await expect(page.getByRole('link', { name: 'Dashboard' })).toHaveCount(0)

  await page.goto('/register')
  await page.getByLabel('Nombre').fill('Usuario E2E')
  await page.getByLabel('Correo').fill(email)
  await page.getByLabel('Contraseña').fill('contraseña-e2e-segura')
  await page.getByRole('button', { name: 'Registrarme' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
  await expect(page.getByText('Aún no tienes planes')).toBeVisible()

  const endpointTimings = await page.evaluate(async () => {
    const urls = [
      '/api/v1/dashboard',
      '/api/v1/meal-plans?page=0&size=20',
      '/api/v1/shopping-lists?page=0&size=20',
      '/api/v1/activity?page=0&size=20',
      '/api/v1/products?supermarketCode=MERCADONA&page=0&size=20',
    ]
    const results: Array<{ url: string; status: number; milliseconds: number }> = []
    for (const url of urls) {
      const startedAt = performance.now()
      const response = await fetch(url, { credentials: 'include' })
      await response.text()
      results.push({
        url,
        status: response.status,
        milliseconds: Math.round((performance.now() - startedAt) * 10) / 10,
      })
    }
    return results
  })
  await testInfo.attach('endpoint-performance.json', {
    body: JSON.stringify(endpointTimings, null, 2),
    contentType: 'application/json',
  })
  for (const timing of endpointTimings) {
    expect(timing.status, timing.url).toBe(200)
    expect(timing.milliseconds, timing.url).toBeLessThan(300)
  }

  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth,
  )
  expect(overflow).toBe(false)
  await page.screenshot({
    path: `test-results/${testInfo.project.name}-dashboard.png`,
    fullPage: true,
  })
})

test('mobile drawer and public catalogue remain keyboard reachable', async ({ page }) => {
  await page.goto('/')
  const menu = page.getByRole('button', { name: 'Abrir menú' })
  if (await menu.isVisible()) {
    await menu.focus()
    await page.keyboard.press('Enter')
    await expect(page.getByLabel('Menú móvil')).toBeVisible()
  }
  await page.goto('/products')
  await expect(
    page.getByRole('heading', { name: 'Encuentra productos que encajen contigo' }),
  ).toBeVisible()
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth,
  )
  expect(overflow).toBe(false)
})
