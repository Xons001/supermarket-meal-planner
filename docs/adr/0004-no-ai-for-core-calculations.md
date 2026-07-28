# ADR 0004 — Sin IA para cálculos centrales

- Estado: aceptado.
- Fecha: 2026-07-28.

## Contexto

Precios, paquetes, nutrición y restricciones deben ser reproducibles,
auditables y comprobables.

## Decisión

El núcleo será determinista. Ningún modelo de lenguaje calculará costes,
nutrientes, cantidades o cumplimiento de restricciones.

## Consecuencias

Una IA futura podrá interpretar preferencias o redactar explicaciones a través
de un puerto opcional, sin acoplar el dominio a OpenAI ni a otro proveedor.
