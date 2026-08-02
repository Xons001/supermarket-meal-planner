#!/usr/bin/env bash
set -euo pipefail
dump="${1:?Uso: restore.sh <archivo.dump> <base-destino> [postgres|airflow-postgres]}"
destination="${2:?La base de destino es obligatoria}"
service="${3:-postgres}"
[[ "$destination" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || { echo "Nombre de base no válido" >&2; exit 2; }
main_database="${POSTGRES_DB:-meal_planner}"
if [[ "$service" == "postgres" && "$destination" == "$main_database" && "${ALLOW_PRODUCTION_RESTORE:-false}" != "true" ]]; then
  echo "Restauración sobre la base principal rechazada. Usa ALLOW_PRODUCTION_RESTORE=true conscientemente." >&2
  exit 3
fi
"$(dirname "$0")/verify-backup.sh" "$dump" "$service"
if [[ "$service" == "postgres" && "$destination" == "$main_database" ]]; then
  "$(dirname "$0")/backup.sh" app
fi
temp="/tmp/$(basename "$dump")"
container_id="$(docker compose ps -q "$service")"
docker cp "$dump" "$container_id:$temp"
docker compose exec -T "$service" sh -ec "PGPASSWORD=\"\$POSTGRES_PASSWORD\" createdb -U \"\$POSTGRES_USER\" '$destination' 2>/dev/null || true; PGPASSWORD=\"\$POSTGRES_PASSWORD\" pg_restore -U \"\$POSTGRES_USER\" -d '$destination' --clean --if-exists --no-owner '$temp'"
docker compose exec -T "$service" rm -f "$temp"
echo "Restauración completada en destino explícito: $destination"
