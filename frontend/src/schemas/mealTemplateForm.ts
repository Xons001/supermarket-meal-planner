import { z } from 'zod'

const positiveNumberText = (label: string) =>
  z
    .string()
    .min(1, `${label} es obligatorio`)
    .refine((value) => Number.isFinite(Number(value)) && Number(value) > 0, {
      message: `${label} debe ser mayor que cero`,
    })

const nonNegativeNumberText = (label: string) =>
  z
    .string()
    .min(1, `${label} es obligatorio`)
    .refine((value) => Number.isInteger(Number(value)) && Number(value) >= 0, {
      message: `${label} debe ser un entero no negativo`,
    })

export const mealTemplateFormSchema = z
  .object({
    supermarketCode: z.string().min(1, 'Selecciona un supermercado'),
    name: z.string().trim().min(2, 'Escribe un nombre').max(180),
    description: z.string().trim().min(5, 'Escribe una descripción'),
    mealType: z.enum(['BREAKFAST', 'LUNCH', 'SNACK', 'DINNER']),
    preparationMinutes: nonNegativeNumberText('El tiempo'),
    servings: positiveNumberText('Las raciones'),
    active: z.boolean(),
    instructions: z
      .array(z.object({ text: z.string().trim().min(1, 'La instrucción no puede estar vacía') }))
      .min(1, 'Añade una instrucción'),
    ingredients: z
      .array(
        z.object({
          productId: z.string().uuid('Selecciona un producto'),
          productLabel: z.string(),
          quantity: positiveNumberText('La cantidad'),
          quantityUnit: z.enum(['GRAM', 'MILLILITER', 'UNIT']),
          optional: z.boolean(),
          sortOrder: nonNegativeNumberText('El orden'),
          notes: z.string().max(500, 'Máximo 500 caracteres'),
        }),
      )
      .min(1, 'Añade un ingrediente'),
  })
  .superRefine((values, context) => {
    const seen = new Set<string>()
    values.ingredients.forEach((ingredient, index) => {
      if (seen.has(ingredient.productId)) {
        context.addIssue({
          code: 'custom',
          path: ['ingredients', index, 'productId'],
          message: 'Este producto ya está incluido',
        })
      }
      seen.add(ingredient.productId)
    })
    if (!values.ingredients.some((ingredient) => !ingredient.optional)) {
      context.addIssue({
        code: 'custom',
        path: ['ingredients'],
        message: 'Debe existir al menos un ingrediente obligatorio',
      })
    }
  })

export type MealTemplateFormValues = z.infer<typeof mealTemplateFormSchema>
