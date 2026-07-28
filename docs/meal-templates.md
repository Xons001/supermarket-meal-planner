# Plantillas de comidas

## Alcance de la FASE 2

Una plantilla representa una comida reutilizable asociada a un supermercado.
Contiene nombre, descripción, tipo, tiempo, raciones, instrucciones ordenadas,
estado e ingredientes del catálogo. La eliminación es un archivado lógico:
conserva el registro, lo desactiva y lo oculta de las consultas públicas.

La carga inicial de `data/mock/meal-templates.json` es idempotente y contiene
exactamente 16 plantillas activas o inactivas de demostración.

## Unidades compatibles

Cada producto declara `measurementType`:

- `WEIGHT`: acepta `GRAM`; los paquetes `KG` se convierten a gramos.
- `VOLUME`: acepta `MILLILITER`; los paquetes `L` se convierten a mililitros.
- `UNIT`: acepta `UNIT` y necesita nutrición explícita por unidad.

Una unidad incompatible, un producto repetido, un ingrediente inexistente o un
producto de otro supermercado producen un error `400 application/problem+json`.

## Cálculo nutricional

Para peso y volumen:

```text
nutriente usado = nutriente por 100 × cantidad usada / 100
```

Para unidades:

```text
nutriente usado = nutriente por unidad × unidades usadas
```

Los totales internos usan `BigDecimal` con precisión `DECIMAL128`. La respuesta
redondea nutrientes a una cifra decimal y dinero a dos, siempre con
`HALF_UP`. El valor por ración es el total dividido entre `servings`.

## Coste consumido y coste de compra

Esta fase calcula solo el coste proporcional consumido:

```text
coste consumido = precio del paquete × cantidad usada / cantidad base del paquete
```

No redondea a paquetes enteros. Por ejemplo, usar 200 g de un paquete de 500 g
que cuesta 4,00 € aporta 1,60 € de coste consumido. La lista de compra de la
FASE 4 calcula por separado el coste real de paquetes completos, sobrante y
aprovechamiento.

## Datos parciales

Un ingrediente sin nutrición explícita o sin precio utilizable no invalida la
plantilla. El resultado:

- marca `nutritionComplete`, `costComplete` y `calculationComplete`;
- devuelve avisos legibles en la plantilla y en cada ingrediente;
- suma únicamente los valores disponibles;
- evita presentar un total parcial como completo.

Los ingredientes opcionales sí participan en los totales, pero no invalidan
filtros de alérgenos o etiquetas pensados para la composición obligatoria.
Si un alérgeno excluido aparece solo en un ingrediente opcional, la plantilla
permanece en el resultado y recibe una advertencia específica.

## Etiquetas calculadas

Los filtros nutricionales usan valores por ración:

- alto en proteína: al menos 20 g;
- bajo en calorías: como máximo 400 kcal;
- alto en fibra: al menos 6 g.

Estos umbrales son reglas de demostración, no declaraciones nutricionales
regulatorias ni recomendaciones médicas.

Para `VEGAN` y `VEGETARIAN`, todos los ingredientes obligatorios deben tener la
etiqueta. `GLUTEN_FREE` y `LACTOSE_FREE` exigen la etiqueta en todos los
ingredientes obligatorios y, además, ausencia del alérgeno correspondiente.
Los filtros CSV se combinan con semántica AND. Los ingredientes opcionales no
deciden la coincidencia.

## Frontend

- `/meal-templates`: listado, búsqueda, filtros, orden y paginación en URL.
- `/meal-templates/new`: creación y previsualización no persistente.
- `/meal-templates/{id}`: detalle calculado y acciones de estado/archivado.
- `/meal-templates/{id}/edit`: edición completa.

Todos los precios y valores son ficticios. La aplicación no está afiliada con
los supermercados mostrados.
