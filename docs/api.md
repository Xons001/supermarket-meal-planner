# API REST

Base: `/api/v1`. Swagger en `/swagger-ui/index.html` es la fuente ejecutable del
contrato. Los precios y productos de esta fase son ficticios.

## Supermercados

```http
GET /api/v1/supermarkets
```

Devuelve `code`, `name`, `enabled`, `catalogSource`, `countryCode` y
`currencyCode`. Mercadona está habilitado; Carrefour, Lidl y Aldi se muestran
como proveedores futuros deshabilitados.

## Categorías

```http
GET /api/v1/categories
GET /api/v1/categories?supermarketCode=MERCADONA
```

`supermarketCode` es opcional. Solo devuelve categorías activas.

```json
[
  {
    "id": "0fe8b178-3660-45ad-85ab-51a996e7e33d",
    "externalId": "demo-cat-preserves",
    "name": "Conservas",
    "parentCategoryId": null,
    "supermarketCode": "MERCADONA"
  }
]
```

## Etiquetas y alérgenos

```http
GET /api/v1/dietary-tags
GET /api/v1/allergens
```

Ambos devuelven opciones con `id`, `code` y `name`. Las etiquetas disponibles
son `HIGH_PROTEIN`, `VEGETARIAN`, `VEGAN`, `GLUTEN_FREE`, `LACTOSE_FREE`,
`LOW_CALORIE` y `HIGH_FIBER`. Los alérgenos son `GLUTEN`, `MILK`, `EGG`, `FISH`,
`SOY` y `NUTS`.

## Buscar productos

```http
GET /api/v1/products
```

Todos los parámetros son opcionales:

| Parámetro | Tipo | Semántica |
| --- | --- | --- |
| `supermarketCode` | texto | Código de supermercado, sin distinguir mayúsculas |
| `categoryId` | UUID | Categoría existente del supermercado |
| `query` | texto | Contenido case-insensitive en nombre o marca; máximo 120 caracteres |
| `available` | boolean | `true` o `false` |
| `maximumPrice` | decimal | Precio del paquete máximo, no negativo |
| `maximumCalories` | decimal | kcal máximas por 100 g, no negativo |
| `minimumProtein` | decimal | Proteína mínima por 100 g, no negativa |
| `dietaryTags` | CSV | El producto debe tener **todas** las etiquetas |
| `excludedAllergens` | CSV | Excluye si aparece **cualquiera**, sin importar presencia |
| `page` | entero | Índice desde cero; por defecto `0` |
| `size` | entero | Entre 1 y 48; por defecto `12` |
| `sort` | texto | `campo,dirección`; por defecto `name,asc` |

Campos de orden admitidos: `name`, `currentPrice`, `unitPrice` y
`lastSyncedAt`. La dirección es `asc` o `desc`; el identificador se añade como
desempate estable.

Ejemplos:

```http
GET /api/v1/products?query=pollo
GET /api/v1/products?supermarketCode=MERCADONA&available=true
GET /api/v1/products?minimumProtein=20&maximumCalories=250
GET /api/v1/products?dietaryTags=HIGH_PROTEIN,LACTOSE_FREE
GET /api/v1/products?excludedAllergens=MILK,GLUTEN
GET /api/v1/products?page=0&size=12&sort=currentPrice,asc
```

Respuesta:

```json
{
  "content": [
    {
      "id": "c9eaf8a3-2e70-4eac-a451-ded01137a3b7",
      "supermarketCode": "MERCADONA",
      "supermarketName": "Mercadona",
      "categoryId": "0fe8b178-3660-45ad-85ab-51a996e7e33d",
      "categoryName": "Carnes y huevos",
      "externalId": "demo-mercadona-chicken-breast",
      "barcode": "DEMO-000001",
      "name": "Pechuga de pollo",
      "brand": "Marca neutra",
      "description": "Filetes de pechuga de pollo de demostración.",
      "imageUrl": null,
      "currentPrice": 4.75,
      "unitPrice": 9.5,
      "packageQuantity": 500,
      "packageUnit": "G",
      "available": true,
      "source": "DEMO_JSON",
      "lastSyncedAt": "2026-07-28T12:00:00Z",
      "nutrition": {
        "caloriesPer100g": 110,
        "proteinPer100g": 23.1,
        "carbohydratesPer100g": 0,
        "fatPer100g": 1.9,
        "fiberPer100g": 0,
        "sugarPer100g": 0,
        "saltPer100g": 0.13,
        "dataSource": "CONTROLLED_DEMO_DATA",
        "verificationStatus": "DEMO",
        "confidenceScore": 1,
        "updatedAt": "2026-07-28T00:00:00Z"
      },
      "dietaryTags": [
        {
          "id": "30000000-0000-0000-0000-000000000001",
          "code": "HIGH_PROTEIN",
          "name": "Alto en proteína"
        }
      ],
      "allergens": [],
      "demonstrationData": true
    }
  ],
  "page": 0,
  "size": 12,
  "totalElements": 24,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

Los productos sin ficha controlada devuelven `nutrition: null`.

## Detalle

```http
GET /api/v1/products/{id}
```

Devuelve el mismo contrato enriquecido con supermercado, categoría, formato,
nutrición, etiquetas, alérgenos con `presenceType`, disponibilidad, fecha de
sincronización e indicador de demostración.

## Histórico de precios

```http
GET /api/v1/products/{id}/price-history
```

Ordenado del registro más reciente al más antiguo:

```json
[
  {
    "id": "6e795035-b093-4ff3-b4fd-4a07dc6c2df0",
    "price": 4.75,
    "unitPrice": 9.5,
    "recordedAt": "2026-07-28T00:00:00Z",
    "demonstrationData": true
  }
]
```

## Errores

El contrato es `application/problem+json` y no contiene stack traces:

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "size must be between 1 and 48",
  "instance": "/api/v1/products",
  "errorCode": "BAD_REQUEST"
}
```

Producen `400`: supermercado, categoría, etiqueta, alérgeno o sort no válidos;
`page`/`size` fuera de rango; números negativos; UUID, booleanos o decimales con
formato incorrecto. Producto inexistente produce `404`.

## Sistema

```text
GET /actuator/health
GET /v3/api-docs
GET /swagger-ui/index.html
```

## Contratos futuros no implementados

```text
POST   /api/v1/meal-plans/generate
POST   /api/v1/meal-plans
GET    /api/v1/meal-plans
GET    /api/v1/meal-plans/{id}
DELETE /api/v1/meal-plans/{id}
GET    /api/v1/meal-plans/{id}/shopping-list
```

La planificación, lista de compra, autenticación, sincronización Airflow e IA
opcional permanecen fuera del runtime actual.
