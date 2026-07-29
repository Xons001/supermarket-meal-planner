# ADR 0022 — Rotación refresh y revocación familiar

## Estado

Aceptado.

## Decisión

Cada refresh se consume una vez y crea otra sesión de la misma familia. Si se
reutiliza uno reemplazado, se revoca la familia en una transacción que no se
revierte al devolver el Problem Details.

## Consecuencias

Se detecta robo por replay. Clientes concurrentes deben compartir una sola
renovación; la SPA implementa ese mutex.
