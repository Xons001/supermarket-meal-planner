# Edición parcial y regeneración

## Alcance

FASE 6 permite editar un plan persistido sin regenerar la semana completa:
sustituir o regenerar una comida, regenerar conjuntamente los slots
desbloqueados de un día, bloquear comidas y deshacer el último cambio de
contenido. Se reutilizan las restricciones, snapshots y fórmulas de `SCORING` y
`PURCHASE_AWARE_SCORING`.

## Identidad y versiones

`MealPlanDay.id` y `PlannedMeal.id` son la identidad canónica de las acciones.
`dayIndex` y `position` solo son fallback para snapshots históricos.

- `rowVersion`: control optimista interno de JPA.
- `editVersion`: aumenta una vez por operación lógica, incluso si afecta varias
  comidas.
- `contentVersion`: aumenta una vez únicamente al sustituir, regenerar o
  deshacer contenido. Bloquear no la modifica.

Todas las columnas, entidades hijas, resumen y `result_json` se actualizan
dentro de la misma transacción.

## Flujo seguro

1. El cliente solicita alternativas o un preview con `expectedEditVersion`.
2. El backend evalúa el plan completo con una seed explícita o derivada de
   plan, versión y objetivo.
3. Devuelve antes, después, delta, razones, advertencias y un token HMAC.
4. La confirmación acepta solo el token y la versión esperada.
5. El backend valida firma, TTL, plan, objetivo, versión, selección y hash,
   recalcula y persiste una operación lógica.

La misma seed produce el mismo ranking y métricas. La firma puede cambiar por
la fecha de expedición. El secreto HMAC llega exclusivamente mediante
`MEAL_PLAN_PREVIEW_HMAC_SECRET`, debe tener 32 bytes como mínimo y no se registra.

## Regeneración y undo

Una comida bloqueada no se puede sustituir ni regenerar. La regeneración de día
trata las comidas bloqueadas y el resto de la semana como contexto fijo y usa
un beam conjunto para todos los slots editables. Si no existe una selección
distinta y válida se devuelve `NO_VALID_ALTERNATIVE`.

Undo restaura todas las comidas del último evento de contenido todavía no
deshecho, conserva los bloqueos actuales, recalcula el plan y crea un único par
de versiones. El evento original queda marcado para que no pueda deshacerse dos
veces.

## Listas de compra

Cada lista conserva `sourcePlanContentVersion`. Su frescura se calcula al leer:

- `CURRENT`: coincide con la versión actual.
- `OUTDATED`: el plan cambió después de crearla.

Editar no archiva ni reemplaza la lista activa. Solo la petición explícita para
generar otra lista convierte la nueva en activa y conserva la anterior
accesible por ID.

## Errores estables

| HTTP | Código |
| --- | --- |
| 400 | `EDIT_PREVIEW_TOKEN_MALFORMED`, `EDIT_PREVIEW_TOKEN_INVALID_SIGNATURE` |
| 409 | `MEAL_PLAN_VERSION_CONFLICT`, `EDIT_PREVIEW_STALE`, `EDIT_CONCURRENT_MODIFICATION` |
| 422 | `NO_VALID_ALTERNATIVE`, `PLANNED_MEAL_LOCKED`, `MEAL_PLAN_NOT_EDITABLE`, `EDIT_RULE_VIOLATION` |
| 404 | `MEAL_PLAN_NOT_FOUND`, `MEAL_PLAN_DAY_NOT_FOUND`, `PLANNED_MEAL_NOT_FOUND` |

Todos se presentan como `application/problem+json` con `errorCode`.

## Límites

No incluye usuarios, autenticación, inventario, colaboración, OR-Tools, IA,
integraciones externas ni Airflow funcional.
