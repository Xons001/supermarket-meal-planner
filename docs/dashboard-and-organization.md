# Dashboard, organización y experiencia de usuario

## Fuente de verdad y proyecciones

`meal_plans.result_json` sigue siendo la fuente canónica de las métricas de un
plan. Las nuevas columnas económicas son proyecciones para consultar, ordenar y
construir el dashboard sin recalcular. `MealPlanEntity.synchronizeSnapshot`
actualiza el JSON y las columnas resumen dentro de la misma transacción. Si el
snapshot no contiene una métrica, la proyección permanece `null`.

## Dashboard

`GET /api/v1/dashboard` usa agregados SQL y columnas persistidas. No carga días,
comidas ni ingredientes. La lista destacada se elige de forma determinista:

1. Lista activa y `CURRENT` del último plan activo.
2. Lista activa y `CURRENT` más reciente.
3. Lista activa y `OUTDATED` más reciente.
4. Ninguna lista.

## Actividad

`GET /api/v1/activity` pagina la unión de dos fuentes:

- `meal_plan_changes`, fuente exclusiva de sustituciones, regeneraciones,
  bloqueos y undo de FASE 6.
- `user_activity_events`, para creación, archivo, restauración, favoritos,
  duplicación y ciclo de vida de listas.

V11 solo backfillea creaciones con un `created_at` inequívoco. Conserva ese
instante y marca `origin=BACKFILLED`. No deduce archivos desde `updated_at` ni
duplica `meal_plan_changes`.

## Listas: ciclo de vida y selección

`archived` controla el ciclo de vida y `active` identifica la lista seleccionada
para un plan. Restaurar únicamente elimina el archivo y mantiene la lista
inactiva. `PATCH /api/v1/shopping-lists/{id}/activate` es la acción explícita que
desactiva la seleccionada anterior sin archivarla. Regenerar conserva la
anterior como historial inactivo. `freshness` compara la versión de origen con
la versión de contenido actual.

## Duplicación

La copia desplaza todos los días por el mismo delta desde la nueva fecha inicial
y crea IDs nuevos. Copia ingredientes, precios, disponibilidad, métricas,
advertencias, bloqueos y snapshots sin consultar el catálogo. Conserva
`originalMealTemplateId` y `partialGenerationSeed`; esta última es solo
procedencia determinista histórica y nunca funciona como token o autorización.
Las comidas quedan marcadas `DUPLICATED`.

No se copian tokens, listas, actividad, historial, favorito, archivo ni
versiones. `duplicated_from_plan_id` mantiene la referencia al origen.

## Tema e identidad

El visitante guarda `LIGHT`, `DARK` o `SYSTEM` localmente. Al iniciar sesión
prevalece la preferencia persistida. Al cerrar sesión o cambiar de identidad se
recupera la preferencia local del visitante. Un script previo a React evita
parpadeos.

## E2E aislado

`scripts/run-e2e.ps1` crea secretos aleatorios, levanta un proyecto Compose
independiente, ejecuta Chromium a 390, 768, 1024 y 1440 px y siempre termina con
`down --volumes`. Los tests crean sus propias cuentas y no dependen de los tres
usuarios manuales ni de su plan privado.

Los tres usuarios manuales y el plan privado se conservan temporalmente. Su
limpieza futura deberá ser una operación explícita; FASE 8 no la ejecuta.

El rate limiting continúa en memoria y no coordina contadores entre nodos.

## Rendimiento del frontend

Las rutas privadas se cargan de forma diferida. Frente a `HEAD`, el chunk
JavaScript inicial de producción pasó de 490.013 a 343.537 bytes: 146.476 bytes
menos (aproximadamente un 29,9 %). Dashboard, actividad, planes y listas quedan
en chunks separados y solo se descargan cuando se visitan.

La verificación con el `package-lock.json` actual devuelve `npm audit`: 0
vulnerabilidades.

En el perfil Docker E2E, con cuatro viewports ejecutados en paralelo, los GET
autenticados quedaron en estos intervalos: dashboard 16–24 ms, planes
14,1–16,8 ms, listas 14,4–15,7 ms, actividad 12,9–16,2 ms y catálogo
36,1–47,7 ms. Son mediciones locales de aceptación, no un SLA de producción.
