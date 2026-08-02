# Sincronización de catálogo y precios

La FASE 9 incorpora Apache Airflow 3.3 con `LocalExecutor`. Airflow es el propietario de las sincronizaciones en runtime; el importador Java se conserva únicamente para bootstrap y compatibilidad.

## Flujos

- `catalog_full_sync` se ejecuta a las 03:00 (`Europe/Madrid`). Normaliza categorías y productos, valida, carga staging, hace merge por lotes y solo marca ausencias tras un `SUCCESS` completo.
- `catalog_price_sync` se ejecuta cada seis horas. Registra historial únicamente cuando cambia el precio efectivo y solo cambia disponibilidad si el proveedor la declara.
- `catalog_sync_cleanup` elimina staging y archivos temporales antiguos. Nunca elimina runs, errores ni historial de precios.

Los DAG no transportan catálogos por XCom: solo IDs, rutas temporales y contadores. Un registro inválido genera un error estructurado y permite `PARTIAL_SUCCESS`; un fallo estructural termina en `FAILED`.

## Proveedores e idempotencia

`LOCAL_JSON` es el proveedor funcional y reproducible. `MERCADONA_EXPERIMENTAL` está aislado y deshabilitado; FASE 9 no realiza scraping. Las claves externas por supermercado, el ID del run y hashes canónicos evitan duplicados. Reintentar crea un run nuevo relacionado con el anterior.

Un producto observado sin cambios actualiza `last_seen_at` y `last_synced_at`, no `updated_at`. Solo un catálogo completo sin errores marca productos ausentes. Una reaparición elimina `unavailable_since`. Nutrición, alérgenos y etiquetas permanecen intactos.

## Operación local

Se requieren secretos externos de Airflow documentados en `.env.example`. `docker compose up --build` inicia PostgreSQL de aplicación, PostgreSQL de metadatos, API, scheduler, procesador de DAG, triggerer, backend y frontend.

- Panel: `http://localhost:5173/admin/catalog-sync`
- Airflow: `http://localhost:8082`
- API ADMIN: `/api/v1/admin/catalog-syncs`

El panel requiere una cuenta `ADMIN`, aplica CSRF y limita las ejecuciones manuales. El rate limiting continúa siendo local al proceso y no es adecuado para despliegues multinodo sin almacenamiento compartido.
