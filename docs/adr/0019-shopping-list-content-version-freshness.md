# ADR 0019 — Frescura de listas por versión de contenido

## Decisión

Guardar `sourcePlanContentVersion` en cada lista y derivar `CURRENT` o
`OUTDATED` al compararla con el plan. Una edición no archiva la lista activa;
solo generar explícitamente una nueva sustituye la activa.

## Consecuencias

El usuario conserva acceso a la lista anterior y distingue con precisión un
cambio de contenido de un simple bloqueo.
