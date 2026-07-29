export const queryKeys = {
  auth: ['auth', 'me'] as const,
  dashboard: ['dashboard'] as const,
  activity: (type: string, page: number) => ['activity', type, page] as const,
  mealPlans: (filters?: unknown) => ['meal-plans', filters] as const,
  mealPlan: (id: string) => ['meal-plan', id] as const,
  shoppingLists: (filters?: unknown) => ['shopping-lists', filters] as const,
  shoppingList: (id: string) => ['shopping-list', id] as const,
}
