# ADR 0016 — Versionado e identidad de la edición

## Decisión

Usar IDs persistentes de días y comidas como identidad canónica, `@Version`
para concurrencia de fila y contadores de dominio `editVersion` y
`contentVersion`. Cada operación lógica incrementa cada contador aplicable una
sola vez.

## Consecuencias

El cliente detecta previews obsoletos y conflictos de forma explícita. Las
operaciones de día y undo múltiple comparten una única nueva versión.
