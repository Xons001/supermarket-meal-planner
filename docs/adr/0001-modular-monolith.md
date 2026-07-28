# ADR 0001 — Monolito modular

- Estado: aceptado.
- Fecha: 2026-07-28.

## Contexto

El producto necesita consistencia entre catálogo, nutrición, planes y compras,
pero todavía no tiene escala ni equipos que justifiquen distribución.

## Decisión

Usar un único backend Spring Boot con módulos por capacidad y límites
application/domain/infrastructure cuando aporten valor real.

## Consecuencias

Despliegue y transacciones simples. Los límites deben comprobarse por estructura
y pruebas; no se incorporan microservicios, CQRS ni eventos distribuidos.
