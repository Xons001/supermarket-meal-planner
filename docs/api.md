# API REST

Base actual: `/api/v1`. Swagger es la fuente ejecutable del contrato.

## Endpoints actuales

### Listar supermercados

```http
GET /api/v1/supermarkets
```

```json
[
  {
    "code": "MERCADONA",
    "name": "Mercadona",
    "enabled": true,
    "catalogSource": "DEMO_JSON",
    "countryCode": "ES",
    "currencyCode": "EUR"
  }
]
```

### Listar productos

```http
GET /api/v1/products?supermarketCode=MERCADONA&page=0&size=20
```

`supermarketCode` es opcional. `page` empieza en cero y `size` admite 1–100.
El orden actual es nombre e identificador.

```json
{
  "content": [
    {
      "id": "c9eaf8a3-2e70-4eac-a451-ded01137a3b7",
      "supermarketCode": "MERCADONA",
      "categoryName": "Proteínas",
      "externalId": "demo-mercadona-chicken-breast",
      "name": "Pechuga de pollo",
      "brand": "Marca genérica",
      "currentPrice": 4.75,
      "unitPrice": 9.50,
      "packageQuantity": 500.000,
      "packageUnit": "G",
      "available": true,
      "source": "DEMO_JSON",
      "demonstrationData": true,
      "nutrition": {
        "caloriesPer100g": 110.00,
        "proteinPer100g": 23.10,
        "dataSource": "CONTROLLED_DEMO_DATA",
        "verificationStatus": "DEMO",
        "confidenceScore": 1.000
      }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 12,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Obtener producto

```http
GET /api/v1/products/{id}
```

Devuelve el mismo contrato de producto completo. Un UUID inexistente produce
404.

## Errores

Formato `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Resource not found",
  "status": 404,
  "detail": "Product not found: ...",
  "instance": "/api/v1/products/...",
  "errorCode": "NOT_FOUND"
}
```

Los parámetros no válidos producen 400 con el mismo formato.

## Sistema

```text
GET /actuator/health
GET /v3/api-docs
GET /swagger-ui/index.html
```

## Endpoints futuros

```text
GET    /api/v1/supermarkets/{code}
GET    /api/v1/products/search
GET    /api/v1/products/{id}/price-history

POST   /api/v1/meal-plans/generate
POST   /api/v1/meal-plans
GET    /api/v1/meal-plans
GET    /api/v1/meal-plans/{id}
DELETE /api/v1/meal-plans/{id}

POST   /api/v1/meal-plans/{id}/regenerate
POST   /api/v1/meal-plans/{id}/meals/{mealId}/replace
POST   /api/v1/meal-plans/{id}/products/{productId}/replace

GET    /api/v1/meal-plans/{id}/shopping-list
GET    /api/v1/meal-plans/{id}/shopping-list/export
```

Los filtros futuros incluyen categoría, texto, disponibilidad, proteína mínima,
calorías máximas, precio máximo, etiqueta dietética y alérgeno.

## Generación futura

Entrada conceptual:

```java
public record GenerateMealPlanCommand(
    String supermarketCode,
    int numberOfDays,
    int mealsPerDay,
    BigDecimal dailyCaloriesTarget,
    BigDecimal dailyProteinTarget,
    BigDecimal weeklyBudget,
    Set<String> dislikedIngredients,
    Set<String> excludedProductIds,
    Set<String> allergens,
    Set<String> dietaryRestrictions
) {}
```

La salida incluirá días, lista de compra, precio total, resumen nutricional y
avisos. Esta operación no está implementada en la FASE 0.
