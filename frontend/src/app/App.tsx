import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
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

const queryClient = createQueryClient()

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/products" element={<ProductsPage />} />
          <Route path="/products/:id" element={<ProductDetailPage />} />
          <Route path="/meal-templates" element={<MealTemplatesPage />} />
          <Route path="/meal-templates/new" element={<MealTemplateFormPage />} />
          <Route path="/meal-templates/:id" element={<MealTemplateDetailPage />} />
          <Route path="/meal-templates/:id/edit" element={<MealTemplateFormPage />} />
          <Route path="/meal-plans" element={<MealPlansPage />} />
          <Route path="/meal-plans/new" element={<MealPlanFormPage />} />
          <Route path="/meal-plans/:id" element={<MealPlanDetailPage />} />
          <Route path="*" element={<HomePage />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
