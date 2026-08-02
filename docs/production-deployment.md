# Despliegue de producción

El objetivo es una sola máquina Linux con Docker Compose. `docker-compose.prod.yml` es independiente del Compose de desarrollo: solo el proxy `edge` publica 80/443. PostgreSQL, backend, Airflow, Actuator y métricas permanecen en redes privadas.

## Preparación

1. Copia `.env.example` fuera del repositorio y sustituye todos los valores de producción. Genera cada secreto por separado con al menos 32 bytes.
2. Monta certificado y clave mediante `TLS_CERT_PATH` y `TLS_KEY_PATH`. Para Let's Encrypt se recomienda certbot en el host y una recarga controlada de Nginx tras renovar.
3. Define un dominio HTTPS en `PUBLIC_BASE_URL`, `APP_AUTH_ISSUER` y `APP_AUTH_ALLOWED_ORIGINS`.
4. Ejecuta `docker compose -f docker-compose.prod.yml config` y revisa que no haya puertos de base de datos ni Airflow.
5. Arranca con `docker compose -f docker-compose.prod.yml up -d --build`.

Spring arranca con `production` y aborta ante secretos débiles/repetidos, cookies inseguras, CORS no HTTPS, Swagger o seeds activos. Los DAG se crean pausados y se habilitan de forma operativa cuando el proveedor productivo esté configurado.

El proxy redirige HTTP a HTTPS, añade HSTS solo en TLS, aplica CSP sin inline/eval y expone únicamente `/`, `/api` y `/healthz`. El frontend interno escucha como usuario no root en 8080.
