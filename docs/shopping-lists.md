# Listas de compra, paquetes y desperdicio

## Alcance

La FASE 4 transforma un plan semanal persistido en una lista de compra
reproducible. Agrega productos repetidos en todos los días y comidas, calcula
solo paquetes completos y presenta coste real estimado, sobrante y comparación
con el presupuesto. No sustituye productos ni modifica el plan o su scoring.

## Fuente y snapshots

La fuente es `MealPlan.resultJson`. Cada ingrediente de un plan nuevo conserva
`productId`, nombre, marca, categoría, magnitud, cantidad, unidad, formato,
precio, disponibilidad y coste consumido. La generación no consulta el catálogo
ni las plantillas actuales.

El generador escala cada ingrediente a las raciones solicitadas antes de
persistirlo y marca `quantityBasis=MEAL_TOTAL`. Por eso la lista suma esas
cantidades directamente y no vuelve a multiplicarlas por raciones.

La lista vuelve a persistir esos campos. Por tanto, un cambio posterior de
nombre, precio, categoría o disponibilidad no reescribe una lista existente.
Los planes guardados antes de esta fase no tienen todos los campos: se aceptan,
pero sus artículos quedan no calculables y muestran
`PRODUCT_SNAPSHOT_INCOMPLETE`.

## Agregación

Los ingredientes obligatorios se recorren en toda la semana y se agrupan por
`productId`. Las cantidades de un mismo producto deben compartir magnitud,
unidad requerida y unidad de paquete. Una contradicción responde `422` sin
persistir una lista inconsistente.

Las magnitudes admitidas son:

| Magnitud | Unidad de cálculo | Formatos convertibles |
|---|---|---|
| `WEIGHT` | g | `G`, `KG` |
| `VOLUME` | ml | `ML`, `L` |
| `UNIT` | unidad | `UNIT` |

No se hacen conversiones por densidad ni equivalencias entre una unidad y un
peso. Los resúmenes de cantidad se devuelven por magnitud para no sumar gramos,
mililitros y unidades.

## Paquetes y costes

Para cada artículo calculable:

```text
packagesRequired = ceil(requiredQuantity / packageQuantity)
purchasedQuantity = packagesRequired × packageQuantity
leftoverQuantity = purchasedQuantity - requiredQuantity
purchaseCost = packagesRequired × packagePrice
wasteCost = max(purchaseCost - consumedCost, 0)
leftoverPercentage = leftoverQuantity / purchasedQuantity × 100
```

Los intermedios usan `BigDecimal` y `MathContext.DECIMAL128`. Las cantidades se
conservan con hasta tres decimales, el dinero se entrega con dos y los
porcentajes con una cifra decimal. Solo el número de paquetes usa `CEILING`.

Ejemplo: 1.200 g requeridos con paquetes de 500 g a 4,00 €:

```text
paquetes: 3
comprado: 1.500 g
sobrante: 300 g
coste de compra: 12,00 €
porcentaje sobrante: 20,0 %
```

El presupuesto semanal se compara con `totalPurchaseCost`. La diferencia es
`presupuesto - coste de compra`: positiva indica importe disponible y negativa,
exceso. Si algún artículo no es calculable, el resultado presupuestario se
marca como parcial.

## Datos incompletos y disponibilidad

Un artículo permanece visible aunque no tenga formato o precio, o aunque figure
como no disponible. La API no rellena datos desconocidos:

- cálculos dependientes del paquete son `null`;
- `calculationComplete=false`;
- la lista incluye un aviso con código estable;
- los totales suman solo artículos calculables;
- `budgetCalculationComplete=false` si la compra completa no puede estimarse.

Un producto no disponible puede seguir siendo matemáticamente calculable con su
snapshot de formato y precio, pero siempre muestra `PRODUCT_UNAVAILABLE`.
En planes antiguos donde el campo no existía, `available=null` significa
“Disponibilidad desconocida”; no se transforma en una falsa advertencia de no
disponibilidad.

## Persistencia y ciclo de vida

`shopping_lists`, `shopping_list_items` y `shopping_list_warnings` almacenan el
resultado. Un índice parcial garantiza una sola lista activa por plan.

- `POST` crea y devuelve `201`.
- Una segunda creación activa devuelve `409`.
- Regenerar archiva la activa y crea otra transaccionalmente.
- Si la regeneración falla, la anterior permanece activa.
- Archivar es lógico; el detalle histórico continúa disponible.

Los estados son `GENERATED` y `ARCHIVED`.

## Interfaz

El detalle del plan permite crear o abrir su lista. El listado independiente
admite supermercado, estado, fechas, completitud, exceso de presupuesto,
ordenación y paginación. El detalle agrupa por categoría, destaca avisos y
artículos no calculables, muestra los tres resúmenes de magnitud y ofrece:

- regeneración con confirmación;
- archivado con confirmación;
- exportación CSV UTF-8 con BOM;
- vista de impresión mediante estilos `@media print`.

## Límites

- Los precios y productos son datos de demostración.
- No hay consolidación entre planes.
- El coste consumido agregado se deriva con el calculador compartido a partir
  de cantidades, formato y precio del snapshot. Puede diferir unos céntimos de
  `MealPlan.totalConsumedCost`, que conserva la suma histórica redondeada por
  comida; para consistencia se usa `purchaseMetrics.estimatedConsumedCost`.
- No se optimizan formatos alternativos ni ofertas.
- No se consulta disponibilidad en tiempo real.

Una lista guarda la versión de contenido que la originó. Las ediciones dejan
la lista activa accesible pero `OUTDATED`; solo una generación explícita crea
la nueva lista `CURRENT` y retira la anterior como activa.

## Consistencia con la optimización

Generador y lista utilizan el mismo componente puro. Con cantidades y
snapshots completos coinciden exactamente en coste consumido agregado, compra,
desperdicio, porcentaje y envases. Con planes históricos o incompletos se
comparan completitud, valores nulos y advertencias compatibles; no se exige una
cifra que no puede calcularse.

## Propiedad

Crear, consultar, regenerar, exportar o archivar exige ser propietario del plan.
Una FK compuesta valida también en SQL que lista y plan tienen el mismo
`owner_id`. Los IDs ajenos se responden como no encontrados.
