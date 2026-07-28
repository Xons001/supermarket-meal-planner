# ADR 0002 — Abstracción de proveedor de supermercado

- Estado: aceptado.
- Fecha: 2026-07-28.

## Contexto

Mercadona es el primer catálogo, pero la plataforma debe admitir varios
supermercados y no depender de APIs privadas o scraping.

## Decisión

Definir `SupermarketCatalogProvider` con modelos externos neutrales. La primera
implementación lee JSON controlado. Los adaptadores específicos vivirán solo en
infraestructura.

## Consecuencias

El dominio, la API y el frontend usan nombres genéricos. Añadir un proveedor no
requiere renombrar entidades ni alterar el núcleo.
