# ADR 0009 — Snapshots en las listas de compra

- Estado: aceptado
- Fecha: 2026-07-28

## Contexto

Una lista debe poder explicarse después de que cambien productos, categorías,
precios, formatos o plantillas. Consultar el catálogo actual al generarla desde
un plan antiguo alteraría silenciosamente el resultado.

## Decisión

La lista se calcula solo desde `MealPlan.resultJson`. Los planes nuevos
incorporan snapshots de producto y paquete en cada ingrediente. La lista
persiste de nuevo esos datos y sus resultados calculados. Nunca reconstruye la
compra desde entidades vivas.

## Consecuencias

Las listas son auditables y reproducibles. Existe duplicación deliberada de
datos. Un plan anterior sin metadatos suficientes produce una lista parcial con
valores nulos y avisos, en lugar de mezclar datos históricos y actuales.

