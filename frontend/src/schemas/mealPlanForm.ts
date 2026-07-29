import { z } from 'zod'

const numberText = (label: string, minimum: number, maximum?: number) =>
  z
    .string()
    .min(1, `${label} es obligatorio`)
    .refine(
      (value) => {
        const parsed = Number(value)
        return (
          Number.isFinite(parsed) &&
          parsed >= minimum &&
          (maximum === undefined || parsed <= maximum)
        )
      },
      `${label} debe estar entre ${minimum}${maximum === undefined ? '' : ` y ${maximum}`}`,
    )

const optionalNumberText = z
  .string()
  .refine(
    (value) => value === '' || (Number.isFinite(Number(value)) && Number(value) > 0),
    'Introduce un número mayor que cero',
  )

export const mealPlanFormSchema = z.object({
  supermarketCode: z.string().min(1, 'Selecciona un supermercado'),
  name: z.string().trim().min(2, 'Escribe un nombre').max(180),
  startDate: z.string().min(1, 'Selecciona la fecha inicial'),
  numberOfDays: numberText('Los días', 1, 14),
  mealsPerDay: numberText('Las comidas por día', 1, 6),
  servings: numberText('Las raciones', 1, 20),
  dailyCaloriesTarget: numberText('Las calorías', 1),
  dailyProteinTarget: numberText('La proteína', 0),
  weeklyBudget: optionalNumberText,
  maximumPreparationMinutes: optionalNumberText,
  maximumTemplateRepetitions: optionalNumberText,
  varietyPreference: z.enum(['LOW', 'MEDIUM', 'HIGH']),
  allowedMealTypes: z
    .array(z.enum(['BREAKFAST', 'LUNCH', 'SNACK', 'DINNER']))
    .min(1, 'Selecciona al menos un tipo de comida'),
  requiredDietaryTags: z.array(z.string()),
  excludedAllergens: z.array(z.string()),
  excludedTemplateIds: z.array(z.string().uuid()),
  excludedProductIds: z.array(z.string().uuid()),
  allowIncompleteCalculations: z.boolean(),
  strategy: z.enum(['SCORING', 'PURCHASE_AWARE_SCORING']),
  optimizationPreset: z.enum(['BALANCED', 'LOWER_PURCHASE_COST', 'LOWER_WASTE', 'MORE_REUSE']),
  deterministicSeed: z
    .string()
    .refine(
      (value) => value === '' || Number.isSafeInteger(Number(value)),
      'La seed debe ser un número entero',
    ),
})

export type MealPlanFormValues = z.infer<typeof mealPlanFormSchema>
