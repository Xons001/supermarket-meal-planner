# ADR 0011 — Agregación global por producto

- Estado: aceptado
- Fecha: 2026-07-28

## Contexto

Calcular paquetes por comida o por día compra de más y exagera el sobrante.
También dificulta detectar que el mismo producto aparece con unidades
contradictorias.

## Decisión

Recorrer todo el snapshot semanal, agrupar primero por `productId` y calcular
paquetes una sola vez por producto. Peso, volumen y unidades permanecen en
resúmenes independientes. Una incompatibilidad de magnitud o unidad para el
mismo producto produce `422`.

## Consecuencias

La compra representa el total semanal y aprovecha un paquete entre varias
comidas. El cálculo necesita tener disponible el plan completo, asumible para el
límite actual de 14 días y 6 comidas diarias.

