# ADR 0014 — Presupuesto de compra y estrategia dual

## Estado

Aceptado.

## Decisión

`PURCHASE_AWARE_SCORING` es el modo predeterminado y evalúa el presupuesto
contra envases completos. `SCORING` se conserva como baseline compatible. Los
pesos nuevos se seleccionan mediante presets configurables.

## Consecuencias

Los clientes pueden comparar ambos algoritmos con la misma seed. Los campos
presupuestarios históricos conservan su significado y las métricas de compra
usan nombres explícitos.
