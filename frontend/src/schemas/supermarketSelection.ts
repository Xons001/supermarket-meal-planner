import { z } from 'zod'

export const supermarketSelectionSchema = z.object({
  supermarketCode: z.string().min(1, 'Selecciona un supermercado disponible.'),
})

export type SupermarketSelection = z.infer<typeof supermarketSelectionSchema>
