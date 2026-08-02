# Enriquecimiento nutricional (FASE 10)

## Objetivo y límites

La fase incorpora datos nutricionales trazables sin convertir una coincidencia externa en una certeza. El proveedor local es la referencia reproducible. Open Food Facts es opcional, está deshabilitado por defecto y no realiza scraping. No se implementan IA, OCR ni recomendaciones clínicas.

## Modelo y prioridad

V13 permite valores parciales, añade grasa saturada, base (`PER_100_GRAMS`, `PER_100_MILLILITERS` o `PER_UNIT`), completitud, fuente, referencia, fecha de fuente, confianza de 0 a 100, revisión y versión optimista. La prioridad estable es:

1. `MANUAL_OVERRIDE`, que nunca se sobrescribe automáticamente.
2. dato `VERIFIED` aceptado por un administrador;
3. coincidencia automática de alta confianza;
4. dato local controlado o histórico de demostración;
5. dato incompleto o ausente.

Los valores desconocidos siguen siendo `null`; no se sustituyen por cero. `COMPLETE` requiere los cuatro campos esenciales (energía, proteína, carbohidratos y grasa) y una cobertura suficiente de los opcionales. Los cálculos nuevos propagan completitud y advertencias.

## Matching determinista

Primero se intenta código de barras exacto. Después se normalizan nombre y marca eliminando acentos, puntuación, ruido de envase y medidas. El score combina código, nombre, marca, formato, categoría, tipo de medida y completitud, con desempate estable por referencia externa.

- `>= 95`: autoaceptación inicial.
- `>= 75` y `< 95`: revisión manual.
- `< 75`: no se propone.

Los umbrales son configuración, no reglas irrevocables. Expresiones protegidas como “sin gluten”, “sin lactosa” o “integral” limitan el score si desaparecen.

## Airflow e idempotencia

El DAG `nutrition_enrichment` se programa una vez por semana (`0 4 * * 1`, `Europe/Madrid`) y tiene una sola ejecución activa. XCom transporta únicamente `runId`, proveedor y estado. Los candidatos son el staging persistente; la unicidad `(product, provider, externalReference, sourceHash)` evita duplicados. El historial usa un hash de snapshot y tampoco duplica resultados en reintentos.

El flujo observable es: selección de ausentes/baja calidad, lotes, lookup por barcode/nombre, score, autoaceptación, cola de revisión, actualización, historial e informe. Un error de producto produce `PARTIAL_SUCCESS`; un fallo estructural produce `FAILED`.

## Proveedores y caché

`NutritionDataProvider` desacopla el dominio. `LOCAL_JSON` usa el fixture versionado y funciona sin red. `OPEN_FOOD_FACTS` soporta producto v3 por barcode y búsqueda nominal heredada con campos mínimos, User-Agent propio, timeout, retraso y reintentos. El cliente externo usa una caché en memoria con TTL: los aciertos positivos usan el TTL configurado, los 404 un cuarto de ese TTL y los errores temporales no se guardan. La tabla `nutrition_provider_cache` queda preparada para una futura caché compartida multinodo; la ejecución local reproducible no la necesita.

Open Food Facts publica límites distintos para lectura de producto y búsqueda. Por ello el proveedor externo permanece opt-in, con `APP_OPENFOODFACTS_USER_AGENT` identificable y una cadencia conservadora.

## Administración y seguridad

Todas las mutaciones requieren sesión `ADMIN`, CSRF y límite configurable de ejecuciones por hora. Un `USER` puede leer el catálogo enriquecido, pero recibe 403 en administración. El panel `/admin/nutrition` muestra resumen, polling de runs, candidatos y decisiones accesibles. Cada aceptación, rechazo y corrección comprueba vigencia/versión y escribe auditoría estructurada sin payloads sensibles.

## Compatibilidad temporal

Los planes ya persistidos conservan sus snapshots y no se recalculan. Los planes futuros usan la nutrición vigente al generar y vuelven a congelarla. Así, enriquecer el catálogo mejora resultados futuros sin alterar decisiones históricas.

## Operación

Variables principales: `APP_NUTRITION_ENRICHMENT_ENABLED`, `APP_NUTRITION_ENRICHMENT_CRON`, `APP_NUTRITION_PROVIDER`, `APP_NUTRITION_BATCH_SIZE`, umbrales, TTL, cooldown y límite manual. Open Food Facts usa las variables `APP_OPENFOODFACTS_*` y queda deshabilitado por defecto.

No se afirma rendimiento para 1.000 productos: ese benchmark no se ha ejecutado. El rate limiting de administración sigue siendo en memoria y, por tanto, no es coordinado entre nodos.
