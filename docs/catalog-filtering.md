# Filtrado del catálogo

## Flujo

El controlador recibe parámetros simples. `ProductSearchRequestParser`
normaliza códigos, valida referencias y construye `ProductSearchCriteria` y un
`Pageable` con orden permitido. `ProductSpecifications` convierte cada filtro
presente en un predicado SQL. El servicio aplica una única Specification,
enriquece el resultado por lotes y lo mapea a DTOs.

No existe un método de repositorio distinto por combinación.

## Combinación

Los filtros se combinan con semántica AND. Por ejemplo:

```text
available=true
minimumProtein=20
maximumCalories=250
dietaryTags=HIGH_PROTEIN,LACTOSE_FREE
excludedAllergens=FISH,SOY
```

exige simultáneamente disponibilidad, rangos nutricionales, ambas etiquetas y
ausencia de pescado y soja.

La búsqueda `query` es case-insensitive y busca contenido en nombre o marca.
Los caracteres `%`, `_` y `\` se tratan como texto, no como comodines.

## Etiquetas dietéticas

`dietaryTags=A,B` significa «contiene A **y** B». Cada etiqueta se implementa
mediante una subconsulta `EXISTS`, por lo que pedir más etiquetas reduce el
conjunto.

## Alérgenos excluidos

`excludedAllergens=A,B` significa «no contiene A **ni** B». Un producto queda
fuera si cualquiera aparece en `product_allergens`, ya sea `CONTAINS`,
`MAY_CONTAIN`, `TRACES` o `UNKNOWN`. Es una exclusión conservadora apropiada
para el catálogo, no una recomendación médica.

## Nutrición ausente

Un producto sin ficha nutricional sigue apareciendo en búsquedas generales.
Cuando se usa `maximumCalories` o `minimumProtein`, solo puede cumplir el filtro
un producto con la cifra disponible.

## Paginación y ordenación

- `page`: desde cero.
- `size`: 12 por defecto; mínimo 1 y máximo 48.
- `sort`: `name`, `currentPrice`, `unitPrice` o `lastSyncedAt`, seguido de
  `asc`/`desc`.
- El UUID se añade como segundo criterio para obtener páginas estables.

El frontend usa los mismos valores en su query key de TanStack Query y conserva
el resultado anterior mientras llega la siguiente página.

## URL del frontend

La UI usa nombres breves y los traduce al contrato API:

| Frontend | API |
| --- | --- |
| `supermarket` | `supermarketCode` |
| `category` | `categoryId` |
| `tags` | `dietaryTags` |
| `exclude` | `excludedAllergens` |

Al recargar `/products`, los controles se reconstruyen desde esos query
parameters. El texto se aplica tras 400 ms de debounce.
