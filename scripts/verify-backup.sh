#!/usr/bin/env bash
set -euo pipefail
dump="${1:?Uso: verify-backup.sh <archivo.dump> [postgres|airflow-postgres]}"
service="${2:-postgres}"
sha256sum --check "$dump.sha256"
temp="/tmp/$(basename "$dump")"
container_id="$(docker compose ps -q "$service")"
docker cp "$dump" "$container_id:$temp"
docker compose exec -T "$service" pg_restore --list "$temp" >/dev/null
docker compose exec -T "$service" rm -f "$temp"
echo "Backup verificado: $dump"
