import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent, { type UserEvent } from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '../app/queryClient'
import {
  mealTemplateFixture,
  productFixture,
  supermarketsFixture,
} from '../test/mealTemplateFixtures'
import { MealTemplateFormPage } from './MealTemplateFormPage'

function mockApi() {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (url.includes('/actuator/health')) return Promise.resolve(Response.json({ status: 'UP' }))
    if (url.includes('/api/v1/supermarkets'))
      return Promise.resolve(Response.json(supermarketsFixture))
    if (url.includes('/api/v1/products')) {
      return Promise.resolve(
        Response.json({
          content: [productFixture],
          page: 0,
          size: 6,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        }),
      )
    }
    if (url.endsWith('/preview') && init?.method === 'POST') {
      return Promise.resolve(Response.json(mealTemplateFixture))
    }
    if (url.endsWith('/api/v1/meal-templates') && init?.method === 'POST') {
      return Promise.resolve(Response.json(mealTemplateFixture, { status: 201 }))
    }
    if (url.endsWith(`/api/v1/meal-templates/${mealTemplateFixture.id}`)) {
      if (init?.method === 'PUT') return Promise.resolve(Response.json(mealTemplateFixture))
      return Promise.resolve(Response.json(mealTemplateFixture))
    }
    return Promise.resolve(Response.json({ detail: 'No encontrado' }, { status: 404 }))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function renderPage(initialEntry = '/meal-templates/new') {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/meal-templates/new" element={<MealTemplateFormPage />} />
          <Route path="/meal-templates/:id/edit" element={<MealTemplateFormPage />} />
          <Route path="/meal-templates/:id" element={<h1>Detalle guardado</h1>} />
          <Route path="/meal-templates" element={<h1>Listado</h1>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

async function fillValidForm(user: UserEvent) {
  await user.type(screen.getByLabelText('Nombre'), 'Plantilla creada')
  await user.type(screen.getByLabelText('Descripción'), 'Descripción suficientemente larga')
  await user.type(screen.getByLabelText('Instrucción 1'), 'Cocer y servir')
  const search = screen.getByRole('searchbox', {
    name: 'Buscar producto del ingrediente 1',
  })
  await user.type(search, 'ar')
  await user.click(
    await screen.findByRole('button', { name: /arroz largo.*despensa demo/i }, { timeout: 1500 }),
  )
}

describe('MealTemplateFormPage', () => {
  beforeEach(() => mockApi())
  afterEach(() => vi.unstubAllGlobals())

  it('renders creation fields and manages dynamic instructions and ingredients', async () => {
    const user = userEvent.setup()
    renderPage()
    expect(screen.getByRole('heading', { name: 'Crear plantilla' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Añadir paso' }))
    expect(screen.getByLabelText('Instrucción 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Añadir ingrediente' }))
    expect(
      screen.getByRole('searchbox', { name: 'Buscar producto del ingrediente 2' }),
    ).toBeInTheDocument()
    const removeButtons = screen.getAllByRole('button', { name: 'Eliminar' })
    await user.click(removeButtons.at(-1)!)
    expect(
      screen.queryByRole('searchbox', { name: 'Buscar producto del ingrediente 2' }),
    ).not.toBeInTheDocument()
  })

  it('validates required fields before submitting', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: 'Crear plantilla' }))
    expect(await screen.findByText('Escribe un nombre')).toBeInTheDocument()
    expect(screen.getByText('Escribe una descripción')).toBeInTheDocument()
    expect(screen.getByText('Selecciona un producto')).toBeInTheDocument()
  })

  it('uses a debounced remote product selector and infers the compatible unit', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage()
    const search = screen.getByRole('searchbox', {
      name: 'Buscar producto del ingrediente 1',
    })
    await user.type(search, 'ar')
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('query=ar'))).toBe(false)
    const result = await screen.findByRole(
      'button',
      { name: /arroz largo.*despensa demo/i },
      { timeout: 1500 },
    )
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('query=ar'))).toBe(true)
    await user.click(result)
    expect(screen.getByText('Seleccionado: Arroz largo')).toBeInTheDocument()
    expect(screen.getByLabelText('Unidad')).toHaveValue('GRAM')
  })

  it('previews calculations without navigating or persisting', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage()
    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Calcular previsualización' }))
    expect(
      await screen.findByRole('heading', { name: 'Previsualización del cálculo' }),
    ).toBeInTheDocument()
    expect(screen.getByText('900 kcal')).toBeInTheDocument()
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) => String(url).endsWith('/preview') && init?.method === 'POST',
      ),
    ).toBe(true)
    expect(screen.getByRole('heading', { name: 'Crear plantilla' })).toBeInTheDocument()
  })

  it('detects a duplicated product before calling the API', async () => {
    const user = userEvent.setup()
    renderPage()
    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Añadir ingrediente' }))
    const secondSearch = screen.getByRole('searchbox', {
      name: 'Buscar producto del ingrediente 2',
    })
    await user.type(secondSearch, 'ar')
    await user.click(
      await screen.findByRole('button', { name: /arroz largo.*despensa demo/i }, { timeout: 1500 }),
    )
    await user.click(screen.getByRole('button', { name: 'Calcular previsualización' }))
    expect(await screen.findByText('Este producto ya está incluido')).toBeInTheDocument()
  })

  it('submits a valid creation and navigates to its detail', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage()
    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Crear plantilla' }))
    expect(await screen.findByRole('heading', { name: 'Detalle guardado' })).toBeInTheDocument()
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) => String(url).endsWith('/api/v1/meal-templates') && init?.method === 'POST',
      ),
    ).toBe(true)
  })

  it('loads an existing template and sends an update', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderPage(`/meal-templates/${mealTemplateFixture.id}/edit`)
    expect(await screen.findByDisplayValue(mealTemplateFixture.name)).toBeInTheDocument()
    expect(screen.getByDisplayValue('Cocer el arroz.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(
          ([url, init]) =>
            String(url).endsWith(`/meal-templates/${mealTemplateFixture.id}`) &&
            init?.method === 'PUT',
        ),
      ).toBe(true),
    )
    expect(await screen.findByRole('heading', { name: 'Detalle guardado' })).toBeInTheDocument()
  })
})
