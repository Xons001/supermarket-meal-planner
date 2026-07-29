# ADR 0025 — Proyecciones de dashboard y actividad combinada

## Estado

Aceptado.

## Decisión

`result_json` continúa como fuente canónica de métricas. Solo se denormalizan
campos necesarios para búsqueda, orden y dashboard, y se sincronizan con el JSON
en la misma transacción.

La actividad se consulta como una unión paginada. `meal_plan_changes` conserva
las ediciones de FASE 6 y `user_activity_events` almacena eventos de organización.
No se copian cambios entre ambas tablas.

## Consecuencias

El dashboard no recalcula ni carga colecciones. Las pruebas deben detectar
divergencias entre snapshot y columnas. Los eventos históricos solo se crean
cuando existe un timestamp inequívoco.
