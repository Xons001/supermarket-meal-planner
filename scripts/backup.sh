#!/usr/bin/env bash
set -euo pipefail

target="${1:-both}"
output_dir="${BACKUP_DIR:-backups}"
retention_days="${BACKUP_RETENTION_DAYS:-14}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$output_dir"

backup_service() {
  local service="$1" database="$2" user="$3" label="$4" file="smp-${label}-${stamp}.dump"
  docker compose exec -T "$service" sh -ec "PGPASSWORD=\"\$POSTGRES_PASSWORD\" pg_dump -U \"$user\" -d \"$database\" --format=custom --file=/tmp/$file"
  container_id="$(docker compose ps -q "$service")"
  docker cp "$container_id:/tmp/$file" "$output_dir/$file"
  docker compose exec -T "$service" rm -f "/tmp/$file"
  sha256sum "$output_dir/$file" > "$output_dir/$file.sha256"
  printf '{"database":"%s","createdAt":"%s","format":"postgres-custom","checksum":"%s"}\n' \
    "$label" "$stamp" "$(cut -d' ' -f1 "$output_dir/$file.sha256")" > "$output_dir/$file.manifest.json"
}

case "$target" in
  app) backup_service postgres "${POSTGRES_DB:-meal_planner}" "${POSTGRES_USER:-meal_planner}" app ;;
  airflow) backup_service airflow-postgres airflow airflow airflow ;;
  both)
    backup_service postgres "${POSTGRES_DB:-meal_planner}" "${POSTGRES_USER:-meal_planner}" app
    backup_service airflow-postgres airflow airflow airflow
    ;;
  *) echo "Uso: $0 [app|airflow|both]" >&2; exit 2 ;;
esac

find "$output_dir" -type f -mtime "+$retention_days" \( -name 'smp-*.dump' -o -name 'smp-*.sha256' -o -name 'smp-*.manifest.json' \) -delete
