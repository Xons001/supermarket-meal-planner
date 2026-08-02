#!/usr/bin/env bash
set -euo pipefail
cert_dir="${PWD}/certificates/prod-e2e"
mkdir -p "$cert_dir"
docker run --rm -v "$cert_dir:/tls" alpine/openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj '/CN=localhost' -keyout /tls/privkey.pem -out /tls/fullchain.pem
docker run --rm -v "$cert_dir:/tls" alpine chmod 644 /tls/privkey.pem /tls/fullchain.pem
secret() { openssl rand -base64 48 | tr '+/' '-_'; }
export TLS_CERT_PATH="$cert_dir/fullchain.pem" TLS_KEY_PATH="$cert_dir/privkey.pem"
export POSTGRES_PASSWORD="$(secret)" AIRFLOW_METADATA_DB_PASSWORD="$(secret)"
export AIRFLOW_FERNET_KEY="$(openssl rand -base64 32 | tr '+/' '-_')"
export AIRFLOW_API_JWT_SECRET="$(secret)" AIRFLOW_WEBSERVER_SECRET_KEY="$(secret)"
export AIRFLOW_ADMIN_USERNAME=smoke-operator AIRFLOW_ADMIN_PASSWORD="$(secret)"
export APP_AUTH_ACCESS_TOKEN_SECRET="$(secret)" APP_AUTH_REFRESH_TOKEN_SECRET="$(secret)"
export MEAL_PLAN_PREVIEW_HMAC_SECRET="$(secret)" APP_OBSERVABILITY_USER_HASH_SECRET="$(secret)"
export APP_AUTH_ISSUER=https://localhost APP_AUTH_ALLOWED_ORIGINS=https://localhost PUBLIC_BASE_URL=https://localhost
project=supermarket-meal-planner-prod-e2e
export COMPOSE_PROJECT_NAME="$project"
trap 'docker compose --project-name "$project" -f docker-compose.prod.yml -f docker-compose.prod-e2e.yml down --volumes --remove-orphans' EXIT
docker compose --project-name "$project" -f docker-compose.prod.yml -f docker-compose.prod-e2e.yml up --build --abort-on-container-exit --exit-code-from smoke smoke
