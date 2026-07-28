# ADR 0008 — Garantía entre preview y plan guardado

- Estado: aceptado
- Fecha: 2026-07-28

## Contexto

Repetir una generación para guardar podría producir otro plan si cambian los
criterios o el catálogo entre ambas peticiones.

## Decisión

El preview devuelve la seed efectiva y un `generationToken` SHA-256 calculado
sobre criterios y candidatas ordenadas. Guardar exige regenerar con ambos
valores. Si el fingerprint difiere, se devuelve `400 Problem Details` y no se
persiste nada.

## Consecuencias

No se mantiene estado de preview en servidor. El guardado es reproducible y
seguro frente a cambios intermedios, a costa de ejecutar el generador una
segunda vez.
