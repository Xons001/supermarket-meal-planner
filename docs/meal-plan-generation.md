# Generación semanal determinista

## Entrada

El motor recibe supermercado, fecha, duración (1–14 días), comidas por día
(1–6), raciones, objetivos diarios de calorías y proteína, presupuesto,
tipos de comida y restricciones. `deterministicSeed` es opcional; si falta, el
servidor genera una seed criptográficamente segura y la devuelve.
Las seeds generadas quedan dentro de `Number.MAX_SAFE_INTEGER` para conservarse
sin pérdida en el cliente TypeScript.

Los objetivos son datos introducidos por el usuario. El sistema no formula
recomendaciones médicas.

## Filtrado

Antes de puntuar se valida que existan el supermercado, etiquetas, alérgenos y
UUID excluidos. Después se descartan plantillas inactivas, archivadas, de otro
supermercado o tipo, demasiado lentas, excluidas, con productos obligatorios
excluidos, alérgenos obligatorios prohibidos, etiquetas incompatibles o datos
insuficientes.

Solo se calculan ingredientes obligatorios. Los opcionales no eliminan una
plantilla, no se añaden automáticamente y no afectan nutrición ni coste del plan.
Esto evita convertir una opción de presentación en una decisión silenciosa.

Si no queda una solución, la API devuelve `422 application/problem+json` con
conteos por tipo, descartes por razón, restricciones en conflicto y sugerencias
seguras.

## Posiciones y tipos

La distribución por defecto es determinista:

| Comidas | Posiciones |
|---:|---|
| 1 | LUNCH |
| 2 | LUNCH, DINNER |
| 3 | BREAKFAST, LUNCH, DINNER |
| 4 | BREAKFAST, LUNCH, SNACK, DINNER |
| 5 | BREAKFAST, SNACK, LUNCH, SNACK, DINNER |
| 6 | BREAKFAST, SNACK, LUNCH, SNACK, DINNER, SNACK |

Si un tipo no está permitido se sustituye por un tipo permitido de forma
estable y se añade `MEAL_TYPE_ADAPTED`. Nunca se inventan tipos.

## Algoritmo

`ScoringMealPlanGenerationStrategy` usa beam search:

1. Recalcula cada candidata con ingredientes obligatorios.
2. Ordena de forma estable por penalización y desempate derivado de la seed.
3. Construye el plan posición a posición.
4. Conserva los 24 mejores estados parciales.
5. Evalúa como máximo 8 candidatas por posición.
6. Puntúa los planes completos y elige el mejor.

El límite evita crecimiento combinatorio y bloqueos. Con los datos demo, una
semana de 7 × 4 se genera claramente por debajo del objetivo de dos segundos.

## Scoring

Todos los factores se normalizan a 0–100. La puntuación total es la media
ponderada:

| Factor | Peso | Ámbito | Motivo |
|---|---:|---|---|
| Calorías | 25 | comida/día/plan | 80 % premia el total diario en margen ±5 % y 20 % evita concentrar todas las calorías en una comida |
| Proteína | 25 | día/plan | Penaliza el déficit; no penaliza alcanzar o superar el objetivo |
| Presupuesto | 15 | plan | Penalización progresiva solo si se supera |
| Variedad | 15 | plan | Combina plantillas únicas (60 %), ingredientes obligatorios únicos (25 %) y cobertura de tipos de comida (15 %) |
| Repetición | 10 | plan | Exceso, mismo día y días consecutivos |
| Completitud | 5 | plan | Penaliza comidas con datos parciales |
| Preparación | 5 | comida/plan | Favorece tiempos bajos respecto al máximo |

Los pesos, beam width, candidatos y márgenes viven en
`MealPlanScoringProperties` (`app.meal-plans.scoring`), no en controladores.
Los subpesos de distribución nutricional y variedad también son configurables.
Además, la penalización parcial considera objetivos por comida y coste esperado
para evitar mantener candidatos claramente peores.

El scoring conserva el coste proporcional a la cantidad consumida. No equivale
al coste de comprar paquetes completos: la lista de compra de la FASE 4 lo
calcula después de agregar todos los ingredientes del snapshot. Esta separación
mantiene estable la semántica y los pesos del generador.

## Determinismo

La misma petición, datos y seed producen las mismas comidas y cálculos:

- consultas y candidatos se ordenan de forma estable;
- sets se canonicalizan antes de calcular el token;
- no se usan timestamps en ranking o scoring;
- la seed solo interviene en desempates;
- se utiliza un mezclador entero estable, no `Math.random()`;
- `generatedAt` y duración son metadatos, no entradas del algoritmo.

Una seed distinta puede alterar el orden cuando dos candidatas empatan, pero no
desplaza una candidata con una puntuación objetivamente mejor.

## Preview, token y persistencia

`persist=false` genera sin escribir. La respuesta incluye seed y
`generationToken`, un SHA-256 de criterios y snapshot ordenado de candidatas.
Para guardar, el cliente repite la petición con la misma seed, el token y
`persist=true`. El servidor regenera y rechaza el guardado si el token ya no
coincide con los datos disponibles. Así el contenido persistido coincide con el
preview o la operación falla explícitamente.

El guardado es transaccional y almacena:

- campos calculados consultables;
- días, comidas e ingredientes obligatorios;
- advertencias;
- criterios JSON;
- respuesta completa JSON.

Cambios posteriores en productos o plantillas no modifican silenciosamente el
snapshot.

## Advertencias y límites

Se explican desviaciones calóricas mayores del 10 %, déficit de proteína,
presupuesto superado, repetición preferida excedida, cálculos incompletos,
adaptación de tipos y omisión de opcionales.

Los ingredientes de un plan nuevo incluyen snapshots de producto, categoría,
formato, precio y disponibilidad para que la lista no dependa del catálogo
actual. Los planes anteriores siguen siendo legibles y producen listas
parciales cuando esos campos faltan.

## Estrategia purchase-aware

La FASE 5 conserva esta estrategia clásica y añade otra que incorpora al beam
coste real, envases, desperdicio y reutilización económicamente útil. La
repetición descriptiva de un producto no se premia por sí sola.

Los factores y presets están centralizados en configuración. Si no existe
presupuesto, la referencia de 2,50 € por comida solo normaliza scores. Un
`wasteCostDelta` negativo significa que una comida reduce el sobrante ya
comprado y puede mejorar el candidato.

Detalles completos en
[optimización del plan por compra real](purchase-aware-meal-plan-optimization.md).

La edición parcial reutiliza la preparación, filtrado y evaluación de
candidatos. La estrategia y sus pesos originales se conservan en cada plan y
el reevaluador recalcula el resultado completo después de una edición.

Límites actuales: no hay IA, OR-Tools ni recomendaciones médicas.
