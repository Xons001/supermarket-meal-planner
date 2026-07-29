# ADR 0015 — Beam incremental sin OR-Tools

## Estado

Aceptado.

## Decisión

El Beam Search mantiene agregados inmutables por candidato y calcula deltas
solo para los productos añadidos. Los finalistas se recalculan exactamente. No
se incorpora OR-Tools en esta fase.

## Consecuencias

Se conserva determinismo y se limita el coste de expansión. Un
`wasteCostDelta` negativo es una mejora válida y reduce la penalización.
