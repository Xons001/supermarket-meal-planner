# Observabilidad

Cada respuesta devuelve `X-Request-ID`. Un valor entrante seguro se conserva; el resto se reemplaza por UUID. El backend añade request, trace/span local, servicio, entorno, versión y un hash HMAC del usuario al MDC. Nunca registra tokens, cookies, contraseñas, correos completos ni payloads externos.

Producción emite JSON mediante Logback. Los Problem Details incorporan `code` y `correlationId`; un error no controlado responde `500 INTERNAL_ERROR` sin stack trace, que sí queda en el log interno.

Actuator usa el puerto interno 9090. Liveness comprueba el proceso y readiness PostgreSQL/Flyway/configuración. Airflow y el proveedor nutricional pueden quedar `DEGRADED` sin retirar disponibilidad. `/healthz` solo publica `UP|DOWN`.

El overlay `docker-compose.observability.yml` añade Prometheus (15 días) y Grafana enlazado exclusivamente a `127.0.0.1`. Incluye alertas y cuatro dashboards versionados. Las métricas usan etiquetas acotadas; UUID, email y producto están prohibidos como labels.
