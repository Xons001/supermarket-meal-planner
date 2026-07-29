import { lazy, Suspense, type ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import { createQueryClient } from './queryClient'
import { HomePage } from '../pages/HomePage'
import { ProductsPage } from '../pages/ProductsPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { MealTemplatesPage } from '../pages/MealTemplatesPage'
import { MealTemplateDetailPage } from '../pages/MealTemplateDetailPage'
import { AuthProvider } from '../auth/AuthProvider'
import { AdminRoute, ProtectedRoute } from '../auth/ProtectedRoute'
import { AuthPage } from '../pages/AuthPage'
import { StatePage } from '../pages/StatePage'
import { Skeleton, ToastProvider } from '../components/ui'

const MealTemplateFormPage = lazy(() =>
  import('../pages/MealTemplateFormPage').then((module) => ({
    default: module.MealTemplateFormPage,
  })),
)
const MealPlansPage = lazy(() =>
  import('../pages/MealPlansPage').then((module) => ({ default: module.MealPlansPage })),
)
const MealPlanFormPage = lazy(() =>
  import('../pages/MealPlanFormPage').then((module) => ({ default: module.MealPlanFormPage })),
)
const MealPlanDetailPage = lazy(() =>
  import('../pages/MealPlanDetailPage').then((module) => ({ default: module.MealPlanDetailPage })),
)
const ShoppingListsPage = lazy(() =>
  import('../pages/ShoppingListsPage').then((module) => ({ default: module.ShoppingListsPage })),
)
const ShoppingListDetailPage = lazy(() =>
  import('../pages/ShoppingListDetailPage').then((module) => ({
    default: module.ShoppingListDetailPage,
  })),
)
const ProfilePage = lazy(() =>
  import('../pages/ProfilePage').then((module) => ({ default: module.ProfilePage })),
)
const DashboardPage = lazy(() =>
  import('../pages/DashboardPage').then((module) => ({ default: module.DashboardPage })),
)
const ActivityPage = lazy(() =>
  import('../pages/ActivityPage').then((module) => ({ default: module.ActivityPage })),
)

const queryClient = createQueryClient()
const privateRoute = (page: ReactNode) => (
  <ProtectedRoute>
    <Suspense fallback={<RouteLoading />}>{page}</Suspense>
  </ProtectedRoute>
)

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/products" element={<ProductsPage />} />
              <Route path="/products/:id" element={<ProductDetailPage />} />
              <Route path="/meal-templates" element={<MealTemplatesPage />} />
              <Route
                path="/meal-templates/new"
                element={
                  <AdminRoute>
                    <Suspense fallback={<RouteLoading />}>
                      <MealTemplateFormPage />
                    </Suspense>
                  </AdminRoute>
                }
              />
              <Route path="/meal-templates/:id" element={<MealTemplateDetailPage />} />
              <Route
                path="/meal-templates/:id/edit"
                element={
                  <AdminRoute>
                    <Suspense fallback={<RouteLoading />}>
                      <MealTemplateFormPage />
                    </Suspense>
                  </AdminRoute>
                }
              />
              <Route path="/dashboard" element={privateRoute(<DashboardPage />)} />
              <Route path="/meal-plans" element={privateRoute(<MealPlansPage />)} />
              <Route path="/meal-plans/new" element={privateRoute(<MealPlanFormPage />)} />
              <Route path="/meal-plans/:id" element={privateRoute(<MealPlanDetailPage />)} />
              <Route path="/shopping-lists" element={privateRoute(<ShoppingListsPage />)} />
              <Route
                path="/shopping-lists/:id"
                element={privateRoute(<ShoppingListDetailPage />)}
              />
              <Route path="/activity" element={privateRoute(<ActivityPage />)} />
              <Route path="/profile" element={privateRoute(<ProfilePage />)} />
              <Route path="/login" element={<AuthPage mode="login" />} />
              <Route path="/register" element={<AuthPage mode="register" />} />
              <Route
                path="/forbidden"
                element={
                  <StatePage
                    code="403"
                    title="Acceso restringido"
                    description="No tienes permiso."
                  />
                }
              />
              <Route
                path="/offline"
                element={
                  <StatePage
                    code="Sin conexión"
                    title="Backend no disponible"
                    description="Revisa la conexión y vuelve a intentarlo."
                  />
                }
              />
              <Route
                path="/error"
                element={
                  <StatePage
                    code="Error"
                    title="Ha ocurrido algo inesperado"
                    description="Tus datos no se han modificado. Puedes volver a intentarlo."
                  />
                }
              />
              <Route
                path="*"
                element={
                  <StatePage
                    code="404"
                    title="Página no encontrada"
                    description="La dirección no existe o se ha movido."
                  />
                }
              />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  )
}

function RouteLoading() {
  return (
    <main style={{ width: 'min(1100px, calc(100% - 2rem))', margin: '3rem auto' }}>
      <Skeleton height="18rem" />
    </main>
  )
}
