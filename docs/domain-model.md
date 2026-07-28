# Modelo de dominio

## Identificadores y tipos

Se usarán UUID en todas las entidades. Evitan revelar secuencias internas,
permiten fusionar datos procedentes de diferentes proveedores y son apropiados
como identificadores públicos.

- Dinero: `BigDecimal` y `NUMERIC(12,2)`.
- Cantidades: `BigDecimal` y `NUMERIC(12,3)`.
- Nutrición: valores por 100 g con `NUMERIC`.
- Tiempos: `OffsetDateTime`/`TIMESTAMPTZ`, normalizados a UTC.
- Unidades: códigos explícitos (`G`, `KG`, `ML`, `L`, `UNIT`).

Nunca se utilizarán `FLOAT` o `DOUBLE` para dinero.

## Entidades actuales

### Supermarket

`id`, `code`, `name`, `enabled`, `catalogSource`, `countryCode`,
`currencyCode`, `createdAt`, `updatedAt`.

`code` es estable y único. `enabled=false` identifica proveedores próximos.

### Category

`id`, `supermarketId`, `externalId`, `name`, `parentCategoryId`, `active`.

La clave `(supermarket_id, external_id)` es única.

### Product

`id`, `supermarketId`, `categoryId`, `externalId`, `barcode`, `name`, `brand`,
`description`, `imageUrl`, `productUrl`, `currentPrice`, `unitPrice`,
`packageQuantity`, `packageUnit`, `available`, `source`, `lastSyncedAt`,
`createdAt`, `updatedAt`.

Un producto ausente en una sincronización se conserva con `available=false`.
La clave `(supermarket_id, external_id)` evita duplicados.

### Nutrition

`id`, `productId`, calorías, proteína, carbohidratos, grasa, fibra, azúcar y sal
por 100 g, `dataSource`, `verificationStatus`, `confidenceScore`, `updatedAt`.

La relación con producto es uno a uno. Coincidir solo por nombre no convierte un
dato en verificado.

## Entidades de catálogo de la FASE 1

### ProductPriceHistory

`id`, `productId`, `price`, `unitPrice`, `recordedAt`. La clave por producto y
fecha de registro hace idempotente la importación JSON.

### DietaryTag y ProductDietaryTag

`DietaryTag(id, code, name)` y relación
`ProductDietaryTag(productId, dietaryTagId)`.

Los códigos actuales son `HIGH_PROTEIN`, `VEGETARIAN`, `VEGAN`, `GLUTEN_FREE`,
`LACTOSE_FREE`, `LOW_CALORIE` y `HIGH_FIBER`.

### Allergen y ProductAllergen

`Allergen(id, code, name)` y relación
`ProductAllergen(productId, allergenId, presenceType)`.

`presenceType` admite `CONTAINS`, `MAY_CONTAIN`, `TRACES` y `UNKNOWN`. Los
alérgenos controlados son gluten, leche, huevo, pescado, soja y frutos de
cáscara.

## Entidades previstas del planificador

- `MealTemplate`: nombre, tipo, instrucciones, preparación y estado.
- `MealTemplateIngredient`: plantilla, producto, gramos y opcionalidad.
- `MealPlan`: supermercado, periodo, objetivos, presupuesto y estado.
- `MealPlanDay`: fecha, macros diarios y coste consumido estimado.
- `Meal`: tipo, instrucciones, macros y coste consumido.
- `MealItem`: producto, gramos/unidades, macros y coste.
- `ShoppingList`: plan, precio total estimado y creación.
- `ShoppingListItem`: cantidad requerida/comprada, paquetes, precio y sobrante.
- `UserPreference`: gustos, exclusiones, alérgenos y restricciones; posterior a
  usuarios.

Relaciones principales:

```mermaid
erDiagram
    SUPERMARKET ||--o{ CATEGORY : contains
    SUPERMARKET ||--o{ PRODUCT : offers
    CATEGORY ||--o{ PRODUCT : classifies
    PRODUCT ||--o| NUTRITION : has
    PRODUCT ||--o{ PRODUCT_PRICE_HISTORY : records
    PRODUCT ||--o{ PRODUCT_DIETARY_TAG : tagged
    DIETARY_TAG ||--o{ PRODUCT_DIETARY_TAG : classifies
    PRODUCT ||--o{ PRODUCT_ALLERGEN : declares
    ALLERGEN ||--o{ PRODUCT_ALLERGEN : identifies
    MEAL_PLAN ||--|{ MEAL_PLAN_DAY : contains
    MEAL_PLAN_DAY ||--|{ MEAL : contains
    MEAL ||--|{ MEAL_ITEM : contains
    PRODUCT ||--o{ MEAL_ITEM : uses
    MEAL_PLAN ||--|| SHOPPING_LIST : generates
    SHOPPING_LIST ||--|{ SHOPPING_LIST_ITEM : contains
```

## Costes y paquetes

El coste de compra se basará en paquetes enteros:

- paquete: 500 g;
- requerido: 1.200 g;
- paquetes: 3;
- comprado: 1.500 g;
- sobrante: 300 g.

El coste consumido puede mostrarse como métrica, pero el presupuesto se compara
contra el coste real estimado de los paquetes comprados.

## CatalogSyncRun futuro

Registrará `id`, supermercado, inicio, fin, estado, productos leídos, creados,
actualizados, no disponibles, cambios de precio y mensaje de error.
