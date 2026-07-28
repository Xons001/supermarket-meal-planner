# ADR 0012 — Artículos de compra no calculables

- Estado: aceptado
- Fecha: 2026-07-28

## Contexto

Planes antiguos o datos parciales pueden carecer de precio, formato o magnitud.
Excluir esos productos oculta una necesidad de compra; inventar valores hace
falsos los totales.

## Decisión

Conservar siempre el artículo. Sus cantidades de paquete, costes derivados y
porcentajes quedan `null`, `calculationComplete=false` y se añade un aviso
estable. Los totales suman únicamente artículos calculables y el presupuesto se
marca parcial.

Los productos no disponibles también se conservan y se señalan, aunque sus
snapshots permitan calcular costes.

## Consecuencias

La interfaz es honesta sobre el alcance de los totales y el usuario ve todo lo
que exige el plan. Los consumidores de API deben respetar completitud y valores
nulos en vez de interpretar un cero.

