import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import { createQueryClient } from './queryClient'
import { HomePage } from '../pages/HomePage'
import { ProductsPage } from '../pages/ProductsPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { MealTemplatesPage } from '../pages/MealTemplatesPage'
import { MealTemplateDetailPage } from '../pages/MealTemplateDetailPage'
import { MealTemplateFormPage } from '../pages/MealTemplateFormPage'
import { MealPlansPage } from '../pages/MealPlansPage'
import { MealPlanFormPage } from '../pages/MealPlanFormPage'
import { MealPlanDetailPage } from '../pages/MealPlanDetailPage'
import { ShoppingListsPage } from '../pages/ShoppingListsPage'
import { ShoppingListDetailPage } from '../pages/ShoppingListDetailPage'
import { AuthProvider } from '../auth/AuthProvider'
import { AdminRoute, ProtectedRoute } from '../auth/ProtectedRoute'
import { AuthPage } from '../pages/AuthPage'
import { ProfilePage } from '../pages/ProfilePage'

const queryClient = createQueryClient()

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
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
                  <MealTemplateFormPage />
                </AdminRoute>
              }
            />
            <Route path="/meal-templates/:id" element={<MealTemplateDetailPage />} />
            <Route
              path="/meal-templates/:id/edit"
              element={
                <AdminRoute>
                  <MealTemplateFormPage />
                </AdminRoute>
              }
            />
            <Route
              path="/meal-plans"
              element={
                <ProtectedRoute>
                  <MealPlansPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/meal-plans/new"
              element={
                <ProtectedRoute>
                  <MealPlanFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/meal-plans/:id"
              element={
                <ProtectedRoute>
                  <MealPlanDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/shopping-lists"
              element={
                <ProtectedRoute>
                  <ShoppingListsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/shopping-lists/:id"
              element={
                <ProtectedRoute>
                  <ShoppingListDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<AuthPage mode="login" />} />
            <Route path="/register" element={<AuthPage mode="register" />} />
            <Route path="*" element={<HomePage />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
