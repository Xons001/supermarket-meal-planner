# ADR 0010 — Redondeo a paquetes completos

- Estado: aceptado
- Fecha: 2026-07-28

## Contexto

El coste consumido de una comida es proporcional, pero una compra real exige
adquirir envases enteros. Redondear dinero o cantidad antes de decidir paquetes
puede infravalorar la compra.

## Decisión

Normalizar primero la cantidad y aplicar
`ceil(cantidadRequerida / cantidadPaquete)`. Los cálculos intermedios usan
`BigDecimal` con `DECIMAL128`; solo la salida se redondea: cantidades a un
máximo de tres decimales, dinero a dos y porcentajes a uno.

## Consecuencias

Siempre se compra cantidad suficiente y el presupuesto usa el coste de paquetes
enteros. Puede aparecer sobrante incluso cuando el coste proporcional del plan
cumple el presupuesto.

