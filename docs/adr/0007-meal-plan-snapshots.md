# ADR 0007 — Snapshot de planes generados

- Estado: aceptado
- Fecha: 2026-07-28

## Contexto

Productos, precios, nutrición y plantillas pueden cambiar después de guardar un
plan. Recalcular siempre alteraría el historial sin intervención del usuario.

## Decisión

Persistir campos relacionales suficientes para consultas y consistencia, más el
snapshot completo de criterios y resultado en JSON. Cada comida conserva nombre
de plantilla, ingredientes obligatorios, nutrición, coste, score y advertencias.

## Consecuencias

El detalle histórico no cambia silenciosamente. Existe duplicación controlada y
los nuevos campos de respuesta deben mantener compatibilidad de lectura con los
snapshots existentes.
