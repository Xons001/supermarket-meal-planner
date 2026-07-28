# ADR 0005: Unidades explícitas y cálculo determinista de plantillas

- Estado: aceptada
- Fecha: 2026-07-28

## Contexto

Los productos se venden por peso, volumen o unidades. Mezclar esas magnitudes o
deducir nutrición por unidad a partir de valores por 100 g produce resultados
engañosos. Además, las instrucciones deben conservar un orden estable y no
pueden depender de separadores dentro de texto libre.

## Decisión

Cada producto declara `measurement_type` y cada ingrediente usa una
`quantity_unit` compatible. La nutrición por unidad solo se usa cuando está
declarada explícitamente. Los cálculos emplean `BigDecimal`, reglas de redondeo
comunes y estados de completitud con avisos para datos ausentes.

Las instrucciones se guardan en `meal_template_instructions` con una posición
ordenada. Las cantidades viven en `meal_template_ingredients`; no se serializan
como texto ni como JSON opaco.

La eliminación de plantillas es lógica para preservar referencias futuras.

## Consecuencias

- Las incompatibilidades se rechazan antes de persistir.
- Los resultados parciales son visibles y no se confunden con totales completos.
- El modelo admite cambios de texto e instrucciones con integridad relacional.
- El coste de esta fase es proporcional a lo consumido; el coste por paquetes
  completos queda separado para la futura lista de compra.
