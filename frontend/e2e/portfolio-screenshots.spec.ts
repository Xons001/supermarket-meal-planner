import { expect, test } from '@playwright/test'

test('captures the isolated portfolio journey', async ({ page, context }, testInfo) => {
  test.skip(process.env.PORTFOLIO_CAPTURE !== 'true' || testInfo.project.name !== 'chromium-1440')
  const shot = async (name: string) =>
    page.screenshot({
      path: `../docs/images/${name}.png`,
      fullPage: true,
      type: 'png',
    })

  await page.goto('/')
  await shot('landing')
  await page.goto('/products')
  await expect(page.getByRole('heading', { name: /Encuentra productos/ })).toBeVisible()
  await shot('catalog')

  await page.goto('/register')
  await page.getByLabel('Nombre').fill('Portfolio Demo')
  await page.getByLabel('Correo').fill(`portfolio-${Date.now()}@example.test`)
  await page.getByLabel('Contraseña').fill('contraseña-portfolio-segura')
  await page.getByRole('button', { name: 'Registrarme' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
  await expect(page.getByLabel('Resumen')).toBeVisible()
  await shot('dashboard')

  await page.goto('/meal-plans/new')
  await expect(page.getByRole('heading', { name: /Construye tu plan semanal/ })).toBeVisible()
  await shot('generator')
  await page.getByRole('button', { name: /Generar previsualización/ }).click()
  await expect(page.getByRole('button', { name: /Guardar este plan/ })).toBeVisible({
    timeout: 20_000,
  })
  await shot('preview')
  await page.getByRole('button', { name: /Guardar este plan/ }).click()
  await expect(page).toHaveURL(/\/meal-plans\/[0-9a-f-]+$/)
  await expect(page.getByRole('heading', { name: 'Mi plan semanal' })).toBeVisible()
  await shot('plan')
  await page.getByRole('button', { name: 'Cambiar' }).first().click()
  await expect(page.getByLabel('Prioridad')).toBeVisible()
  await shot('editing')
  const generateList = page.getByRole('button', { name: 'Generar lista de compra' })
  if (await generateList.isVisible()) await generateList.click()
  const listLink = page.getByRole('link', { name: 'Ver lista de compra' })
  await expect(listLink).toBeVisible({ timeout: 15_000 })
  await listLink.click()
  await expect(page).toHaveURL(/\/shopping-lists\/[0-9a-f-]+$/)
  await expect(page.getByRole('region', { name: 'Resumen de la lista' })).toBeVisible()
  await shot('shopping-list')

  await context.clearCookies()
  await page.goto('/login')
  await page.getByLabel('Correo').fill(process.env.E2E_ADMIN_EMAIL!)
  await page.getByLabel('Contraseña').fill(process.env.E2E_ADMIN_PASSWORD!)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await page.goto('/admin/catalog-sync')
  await expect(page.getByText('LOCAL_JSON')).toBeVisible()
  await shot('admin-catalog-sync')
  await page.goto('/admin/nutrition')
  await expect(page.getByRole('button', { name: /Enriquecer/ })).toBeVisible()
  await shot('admin-nutrition')
})
