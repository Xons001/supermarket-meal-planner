const moneyFormatter = new Intl.NumberFormat('es-ES', {
  style: 'currency',
  currency: 'EUR',
})

const dateFormatter = new Intl.DateTimeFormat('es-ES', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

const unitLabels: Record<string, string> = {
  G: 'g',
  KG: 'kg',
  ML: 'ml',
  L: 'l',
  UNIT: 'uds.',
}

export function formatMoney(value: number): string {
  return moneyFormatter.format(value)
}

export function formatDate(value: string): string {
  return dateFormatter.format(new Date(value))
}

export function formatPackage(quantity: number, unit: string): string {
  return `${quantity} ${unitLabels[unit] ?? unit}`
}
