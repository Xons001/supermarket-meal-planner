# Optimización del plan por compra real

## Problema

El coste consumido valora únicamente la fracción utilizada de cada producto.
Una comida barata según esa métrica puede obligar a comprar varios envases
completos. `PURCHASE_AWARE_SCORING` agrega los ingredientes por `productId`
durante la búsqueda y compara alternativas con el desembolso real estimado.

`SCORING` sigue disponible sin cambios funcionales. Si la petición no indica
estrategia, se utiliza la nueva. Los presets `BALANCED`,
`LOWER_PURCHASE_COST`, `LOWER_WASTE` y `MORE_REUSE` seleccionan mapas de pesos
controlados; un preset enviado con `SCORING` se normaliza a `null`.

## Paquetes y métricas

El generador y la lista de compra usan el mismo calculador puro. Convierte
kilogramos a gramos, litros a mililitros y conserva unidades discretas. Para
cada producto calculable:

```text
envases = ceil(cantidad requerida / cantidad del envase)
cantidad comprada = envases × cantidad del envase
coste de compra = envases × precio del envase
coste consumido = precio × cantidad requerida / cantidad del envase
coste de sobrante = coste de compra − coste consumido
desperdicio % = coste de sobrante / coste de compra × 100
```

Precio, formato, unidad y disponibilidad proceden del snapshot. Los datos
ausentes producen métricas parciales, valores nulos y advertencias; no se
consulta el catálogo actual ni se inventan valores.

## Coste marginal y reutilización útil

Cada estado del beam mantiene un mapa inmutable agregado. Al añadir una comida:

```text
purchaseCostDelta = compraDespués − compraAntes
wasteCostDelta = sobranteDespués − sobranteAntes
```

`wasteCostDelta` puede ser negativo. Significa que la comida aprovecha
capacidad ya comprada y reduce el coste total atribuido al sobrante; es una
mejora válida y bonifica el candidato.

`reusedProductCount` es descriptivo. La señal `economicallyUsefulReuse` solo
aumenta cuando la reutilización evita un envase, tiene coste marginal cero,
consume sobrante o mejora realmente el aprovechamiento. Repetir ingredientes
sin ese efecto no recibe bonificación y las penalizaciones de variedad y
repetición continúan aplicándose.

## Scoring y presupuesto

Los factores están centralizados en `app.meal-plans.purchase-aware`. El
presupuesto se evalúa contra el coste real de compra. Cumplirlo obtiene el
máximo factor presupuestario; superarlo penaliza y genera una advertencia sin
ocultar el plan.

En ausencia de presupuesto se usa exclusivamente para normalización
`reference-cost-per-meal: 2.50`. Es un valor configurable de demostración, no
un precio comercial ni una restricción nueva.

Todos los pesos suman 100 por preset y combinan nutrición, compra, presupuesto,
desperdicio, reutilización útil, productos, envases, variedad, repetición,
completitud y preparación. Los factores económicos son configurables para
poder calibrarlos con datos reales sin modificar código.

Tras la comparación de aceptación, `BALANCED` conserva estos pesos efectivos:

| Factor | Peso |
| --- | ---: |
| Calorías / proteína | 21 / 21 |
| Compra / consumido / presupuesto | 10 / 2 / 14 |
| Desperdicio económico / porcentual | 10 / 10 |
| Reutilización útil | 4 |
| Productos únicos / envases | 1 / 2 |
| Variedad / repetición | 2 / 1 |
| Completitud / preparación | 1 / 1 |

La configuración inicial daba 6 puntos a cada componente de desperdicio y
elegía un plan de 49,85 € de compra y 12,14 € de sobrante, peor que el clásico
con la misma entrada. La calibración elevó ambos componentes a 10, redujo los
factores que dominaban la selección y mantuvo 42 puntos de nutrición. No se
modificó ninguna fórmula ni peso de `SCORING`.

## Comparación de aceptación

Medición realizada en Docker con el mismo catálogo, restricciones, objetivo,
presupuesto de 70 €, seed `123456`, siete días y cuatro comidas:

| Métrica | SCORING | PURCHASE_AWARE_SCORING |
| --- | ---: | ---: |
| Calorías / proteína | 8753,3 / 749,8 g | 8836,8 / 757,4 g |
| Coste consumido agregado | 38,03 € | 38,48 € |
| Compra real | 48,05 € | 46,90 € |
| Sobrante | 10,02 € (20,9 %) | 8,42 € (18,0 %) |
| Envases / productos únicos | 25 / 13 | 24 / 12 |
| Plantillas únicas / comidas repetidas | 10 / 18 | 9 / 19 |
| Variedad / repetición | 50,19 / 76,00 | 46,72 / 56,00 |
| Tiempo / candidatos | 101 ms / 1996 | 198 ms / 1996 |

El modo de compra reduce el desembolso 1,15 €, el sobrante 1,60 €, un envase y
un producto, a la vez que mejora ligeramente calorías y proteína. El coste es
una plantilla única menos y una comida repetida más; se considera un intercambio
moderado, no una degradación grave, y los controles de variedad siguen activos.

La repetición de la generación produjo el mismo token y secuencia. Tras
persistir ambos planes, las métricas del plan nuevo coincidieron exactamente
con su lista de compra en coste consumido agregado, compra, sobrante, envases y
productos. El escenario con presupuesto exacto de 45,70 € obtuvo factor de
presupuesto 100; con 30 € se marcó un exceso de 16,90 €. Sin presupuesto se
utilizó la referencia configurable `2,50 × 28 = 70,00 €`.

## Beam Search, determinismo y rendimiento

La expansión actualiza solo los productos de la última comida. La penalización
parcial combina las reglas clásicas con métricas económicas proyectadas. Los
mejores estados se recalculan exactamente antes del orden final.

La estrategia conserva ordenaciones estables, seed, UUID como desempate,
canonicalización y ausencia de timestamps dentro de la decisión. El
`generationToken` incluye estrategia, preset, versión, pesos y snapshots.

El objetivo de aceptación es menos de dos segundos para siete días y cuatro
comidas; el objetivo operativo es permanecer por debajo de 500 ms en el
entorno Docker actual.

## Snapshots y consistencia

Los planes nuevos conservan `purchaseMetrics`, el breakdown ampliado, preset,
pesos y versión. Los históricos devuelven esas propiedades como nulas.

La coincidencia con la lista de compra es exacta cuando ambas reciben las
mismas cantidades y snapshots completos. `purchaseMetrics.estimatedConsumedCost`
es la métrica agregada comparable; `totalConsumedCost` se mantiene por
compatibilidad con la suma histórica por comida. Con datos incompletos se
comparan completitud, nulos y advertencias compatibles, no cifras inexistentes.

## Límites

- Solo participan ingredientes obligatorios.
- No se optimizan ofertas, tamaños alternativos ni varios supermercados.
- Los sobrantes no se trasladan entre semanas.
- No hay inventario doméstico, sustituciones, OR-Tools ni IA.
- `npm audit` informa de `GHSA-qwww-vcr4-c8h2` en React Router 7.18.2.
  Afecta al modo RSC/acciones de servidor; este frontend es una SPA Vite sin
  RSC ni acciones de servidor. No existe una versión publicada sin avisos que
  sea preferible: la bajada propuesta por npm reintroduce múltiples avisos
  XSS y de redirección. Se conserva la última 7.18.2 hasta que haya corrección.
