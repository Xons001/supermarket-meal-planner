# ADR 0026 — Selección de listas y duplicación histórica

## Estado

Aceptado.

## Decisión

El archivo de una lista y su selección son estados independientes. Restaurar no
activa; activar es una operación explícita con unicidad por plan.

Duplicar un plan copia el snapshot histórico sin recalcular contra el catálogo.
Crea identidades nuevas, conserva la procedencia determinista y elimina tokens,
historial, actividad, listas y versiones.

## Consecuencias

El usuario puede consultar listas antiguas sin confundirlas con la seleccionada.
Las copias siguen siendo reproducibles aunque cambien precio o disponibilidad.
