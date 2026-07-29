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
      "measurementType": "WEIGHT",
      "costDataComplete": true,
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

Los productos por unidades pueden incluir `nutrition.perUnit` con los siete
nutrientes explícitos. No se deduce ese bloque a partir de valores por 100 g.

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

## Plantillas de comidas

### Listado y filtros

```http
GET /api/v1/meal-templates
```

Parámetros opcionales:

| Parámetro | Tipo | Semántica |
| --- | --- | --- |
| `supermarketCode` | texto | Supermercado habilitado |
| `mealType` | enum | `BREAKFAST`, `LUNCH`, `SNACK`, `DINNER` |
| `active` | boolean | Estado público de la plantilla |
| `query` | texto | Nombre o descripción, máximo 120 caracteres |
| `minimumProtein` | decimal | Proteína mínima por ración |
| `maximumCalories` | decimal | Calorías máximas por ración |
| `maximumPreparationMinutes` | entero | Tiempo máximo |
| `excludedAllergens` | CSV | Ningún ingrediente obligatorio puede contenerlos |
| `dietaryTags` | CSV | Todos los ingredientes obligatorios deben cumplirlas |
| `page` | entero | Índice desde cero; defecto `0` |
| `size` | entero | Entre 1 y 48; defecto `9` |
| `sort` | texto | `name`, `preparationMinutes`, `caloriesPerServing`, `proteinPerServing`, `costPerServing` o `updatedAt` |

El resultado usa el mismo sobre paginado que productos. Las plantillas
archivadas no se devuelven. Los filtros nutricionales se aplican sobre el valor
calculado por ración y solo coinciden con cálculos completos.

### Detalle, CRUD, estado y previsualización

```http
GET    /api/v1/meal-templates/{id}
POST   /api/v1/meal-templates
PUT    /api/v1/meal-templates/{id}
PATCH  /api/v1/meal-templates/{id}/status
DELETE /api/v1/meal-templates/{id}
POST   /api/v1/meal-templates/preview
```

`POST` responde `201`; `DELETE` archiva lógicamente y responde `204`. `preview`
valida y calcula el mismo contrato, pero no persiste nada.

Petición de creación, reemplazo o previsualización:

```json
{
  "supermarketCode": "MERCADONA",
  "name": "Arroz con pollo",
  "description": "Comida completa de demostración.",
  "mealType": "LUNCH",
  "instructions": ["Cocer el arroz.", "Cocinar y mezclar."],
  "preparationMinutes": 30,
  "servings": 2,
  "active": true,
  "imageUrl": null,
  "ingredients": [
    {
      "productId": "c9eaf8a3-2e70-4eac-a451-ded01137a3b7",
      "quantity": 200,
      "quantityUnit": "GRAM",
      "optional": false,
      "sortOrder": 0,
      "notes": null
    }
  ]
}
```

Cambio aislado de estado:

```json
{ "active": false }
```

Fragmento de respuesta calculada:

```json
{
  "id": "88d0edce-22b4-4de5-a3ed-2397315d1300",
  "name": "Arroz con pollo",
  "servings": 2,
  "ingredients": [
    {
      "productName": "Pechuga de pollo",
      "quantity": 200,
      "quantityUnit": "GRAM",
      "calculatedNutrition": {
        "calories": 220.0,
        "protein": 46.2,
        "carbohydrates": 0.0,
        "fat": 3.8,
        "fiber": 0.0,
        "sugar": 0.0,
        "salt": 0.3
      },
      "calculatedConsumedCost": 1.90,
      "calculationComplete": true,
      "warnings": []
    }
  ],
  "totalNutrition": {
    "calories": 220.0,
    "protein": 46.2,
    "carbohydrates": 0.0,
    "fat": 3.8,
    "fiber": 0.0,
    "sugar": 0.0,
    "salt": 0.3
  },
  "nutritionPerServing": {
    "calories": 110.0,
    "protein": 23.1,
    "carbohydrates": 0.0,
    "fat": 1.9,
    "fiber": 0.0,
    "sugar": 0.0,
    "salt": 0.1
  },
  "totalConsumedCost": 1.90,
  "consumedCostPerServing": 0.95,
  "calculationComplete": true,
  "nutritionComplete": true,
  "costComplete": true,
  "warnings": [],
  "demoData": true
}
```

Los importes representan consumo proporcional, no paquetes enteros comprados.
Si faltan nutrición o precio, el contrato devuelve valores parciales,
indicadores `false` y avisos; nunca inventa datos.

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
formato incorrecto. En plantillas también producen `400` las unidades
incompatibles, productos repetidos, productos de otro supermercado e
ingredientes o instrucciones vacíos. Producto o plantilla inexistente produce
`404`.

## Sistema

```text
GET /actuator/health
GET /v3/api-docs
GET /swagger-ui/index.html
```

## Planes semanales

### Generar o guardar

```text
POST /api/v1/meal-plans/generate
```

El cuerpo completo está en
[`docs/examples/generate-meal-plan.json`](examples/generate-meal-plan.json).
Admite 1–14 días, 1–6 comidas y `persist=false|true`. Si la seed se omite,
la respuesta incluye la generada.

La respuesta contiene `generationToken`, criterios normalizados, días,
comidas e ingredientes obligatorios, nutrición diaria y semanal, coste
consumido, presupuesto y desviación, `overallScore`, `scoreBreakdown`,
`varietyMetrics`, advertencias, restricciones y metadatos de generación.

Para guardar exactamente un preview se repite el cuerpo con:

```json
{
  "deterministicSeed": 123456,
  "generationToken": "sha256-del-preview",
  "persist": true
}
```

El resto de criterios debe mantenerse. Una discrepancia de fingerprint produce
`400` y no guarda.

### Listar

```text
GET /api/v1/meal-plans
```

Filtros:

| Parámetro | Valores |
|---|---|
| `supermarketCode` | Código existente |
| `status` | `DRAFT`, `GENERATED`, `ARCHIVED` |
| `startDateFrom`, `startDateTo` | Fecha ISO inclusiva |
| `minimumScore` | 0–100 |
| `page` | Entero desde 0 |
| `size` | 1–48 |
| `sort` | `name`, `startDate`, `overallScore`, `createdAt` o `totalConsumedCost`, más `asc`/`desc` |

### Detalle y estado

```text
GET    /api/v1/meal-plans/{id}
PATCH  /api/v1/meal-plans/{id}/status
DELETE /api/v1/meal-plans/{id}
```

El `PATCH` acepta `{"status":"GENERATED"}` o `{"status":"ARCHIVED"}`. No se
puede devolver un plan persistido a `DRAFT`. `DELETE` realiza archivado lógico
y responde `204`.

### Generación imposible

```json
{
  "title": "Meal plan generation is impossible",
  "status": 422,
  "errorCode": "MEAL_PLAN_GENERATION_IMPOSSIBLE",
  "candidateCounts": {"BREAKFAST": 0},
  "rejectedByReason": {"excludedAllergen": 4},
  "conflictingConstraints": ["excludedAllergens=[MILK]"],
  "suggestions": ["Allow another meal type"]
}
```

Supermercado, etiquetas, alérgenos o UUID excluidos inexistentes; rangos,
objetivos, presupuesto y cambios de estado inválidos producen `400`. Un plan
inexistente produce `404`. No se exponen trazas.

## Listas de compra

### Crear, consultar, regenerar y archivar

```text
POST   /api/v1/meal-plans/{mealPlanId}/shopping-list
GET    /api/v1/meal-plans/{mealPlanId}/shopping-list
POST   /api/v1/meal-plans/{mealPlanId}/shopping-list/regenerate
PATCH  /api/v1/meal-plans/{mealPlanId}/shopping-list/status
DELETE /api/v1/meal-plans/{mealPlanId}/shopping-list
```

`POST` crea desde el snapshot del plan y responde `201`. Si ya existe una lista
activa responde `409`. `regenerate` archiva la anterior y crea el reemplazo
dentro de una transacción; un error de cálculo conserva intacta la lista
anterior. `DELETE` archiva lógicamente y responde `204`. `PATCH` acepta
`{"status":"GENERATED"}` o `{"status":"ARCHIVED"}`.

La respuesta agrupa artículos por categoría e incluye:

- snapshot del plan y supermercado, estado y fechas;
- cantidad requerida, formato, paquetes enteros, comprado y sobrante;
- coste consumido, coste de compra, desperdicio y porcentajes;
- resúmenes separados para `WEIGHT`, `VOLUME` y `UNIT`;
- presupuesto, diferencia respecto a la compra e indicador de exceso;
- completitud y avisos de lista y artículo.

Los campos derivados de un artículo no calculable son `null`; no se inventan
precios ni formatos. Los planes anteriores a la FASE 4 se admiten con resultado
parcial. `available` también puede ser `null` cuando un snapshot histórico no
contenía ese dato.

### Listar, detalle y exportación

```text
GET /api/v1/shopping-lists
GET /api/v1/shopping-lists/{id}
GET /api/v1/shopping-lists/{id}/export?format=csv
```

Filtros del listado:

| Parámetro | Valores |
|---|---|
| `supermarketCode` | Código existente |
| `status` | `GENERATED`, `ARCHIVED` |
| `generatedFrom`, `generatedTo` | Fecha y hora ISO inclusiva |
| `calculationComplete` | `true`, `false` |
| `budgetExceeded` | `true`, `false` |
| `page` | Entero desde 0 |
| `size` | 1–48 |
| `sort` | `generatedAt`, `totalPurchaseCost`, `totalWasteCost` u `overallWastePercentage`, más `asc`/`desc` |

El CSV se descarga como `text/csv;charset=UTF-8`, incluye BOM y usa el snapshot
persistido de la lista.

### Errores específicos

- `404`: plan o lista inexistente.
- `409`: ya existe una lista activa.
- `422`: un mismo producto aparece con magnitudes o unidades incompatibles.
- `400`: filtros, estados o formato de exportación no válidos.

Todos usan `application/problem+json` sin trazas ni información interna.

## Optimización de compra en planes

`POST /api/v1/meal-plans/generate` acepta opcionalmente:

```json
{
  "strategy": "PURCHASE_AWARE_SCORING",
  "optimizationPreset": "BALANCED"
}
```

La estrategia purchase-aware es el valor predeterminado. `SCORING` mantiene el
algoritmo clásico; si recibe `optimizationPreset`, el backend lo normaliza a
`null`.

Preview, persistencia y detalle incorporan `purchaseMetrics`, los nuevos
componentes opcionales de `scoreBreakdown` y en `generationMetadata` el preset
y los pesos efectivos. `purchaseMetrics.estimatedConsumedCost` es comparable
con la lista agregada. Los campos nuevos son nulos en snapshots históricos.

Ejemplo parcial:

```json
{
  "strategy": "PURCHASE_AWARE_SCORING",
  "purchaseMetrics": {
    "estimatedConsumedCost": 37.53,
    "estimatedPurchaseCost": 53.35,
    "estimatedWasteCost": 15.82,
    "estimatedWastePercentage": 29.7,
    "estimatedPackageCount": 25,
    "estimatedUniqueProductCount": 13,
    "purchaseBudgetDifference": 16.65,
    "calculationComplete": true
  }
}
```

## Contratos futuros no implementados

La autenticación, sincronización Airflow, OR-Tools e IA opcional permanecen
fuera del runtime actual.

## Edición parcial de planes persistidos

Todos los objetivos usan los UUID persistentes de día y comida. Las
confirmaciones aceptan exclusivamente `{previewToken, expectedEditVersion}`.

```text
GET  /api/v1/meal-plans/{planId}/meals/{plannedMealId}/alternatives
POST /api/v1/meal-plans/{planId}/meals/{plannedMealId}/replacement-previews
POST /api/v1/meal-plans/{planId}/meals/{plannedMealId}/replacements
POST /api/v1/meal-plans/{planId}/meals/{plannedMealId}/regeneration-previews
POST /api/v1/meal-plans/{planId}/meals/{plannedMealId}/regenerations
POST /api/v1/meal-plans/{planId}/days/{dayId}/regeneration-previews
POST /api/v1/meal-plans/{planId}/days/{dayId}/regenerations
PATCH /api/v1/meal-plans/{planId}/meals/{plannedMealId}/lock
POST /api/v1/meal-plans/{planId}/undo
GET  /api/v1/meal-plans/{planId}/changes?page=0&size=20
```

El bloqueo recibe `{locked, expectedEditVersion}`. Los previews incluyen
comidas y métricas antes/después/delta, razones, advertencias, seed, TTL y token.
El detalle expone `editVersion`, `contentVersion`, IDs persistentes, bloqueos,
origen de selección, estado de lista, `canUndo` y último cambio.

Problem Details usa códigos estables:

- 400: `EDIT_PREVIEW_TOKEN_MALFORMED`,
  `EDIT_PREVIEW_TOKEN_INVALID_SIGNATURE`.
- 409: `MEAL_PLAN_VERSION_CONFLICT`, `EDIT_PREVIEW_STALE`,
  `EDIT_CONCURRENT_MODIFICATION`.
- 422: `NO_VALID_ALTERNATIVE`, `PLANNED_MEAL_LOCKED`,
  `MEAL_PLAN_NOT_EDITABLE`, `EDIT_RULE_VIOLATION`.
- 404: `MEAL_PLAN_NOT_FOUND`, `MEAL_PLAN_DAY_NOT_FOUND`,
  `PLANNED_MEAL_NOT_FOUND`.
