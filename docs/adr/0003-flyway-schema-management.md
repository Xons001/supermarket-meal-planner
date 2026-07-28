# ADR 0003 — Flyway gestiona el esquema

- Estado: aceptado.
- Fecha: 2026-07-28.

## Contexto

La generación automática de Hibernate no ofrece historial revisable ni
despliegues repetibles.

## Decisión

Todas las tablas, índices, restricciones y datos de referencia se crean mediante
Flyway. Hibernate se configura con `ddl-auto=validate`.

## Consecuencias

Cada cambio de modelo requiere una migración explícita. Producción, desarrollo y
Testcontainers ejecutan el mismo esquema PostgreSQL.
