# Backups y restauración

`scripts/backup.{sh,ps1}` crea dumps custom de PostgreSQL principal, metadatos Airflow o ambos. Cada archivo incluye SHA-256 y manifiesto UTC sin credenciales. La retención predeterminada es 14 días.

`verify-backup` comprueba checksum y `pg_restore --list`. `restore` exige nombre de destino explícito y rechaza la base principal; `ALLOW_PRODUCTION_RESTORE=true` o `-AllowProductionRestore` autoriza conscientemente el reemplazo y crea antes otro backup.

La prueba operativa correcta restaura en una base temporal desechable, ejecuta Flyway con `validate`, cuenta usuarios/catálogo/planes/listas y comprueba health. Nunca se usa `flyway clean`.

Con un backup diario el RPO orientativo es 24 horas. El RTO debe anotarse tras cada simulacro real; no se publica una garantía contractual sin una medición reproducible.

## Verificación local de FASE 11

El 2 de agosto de 2026 se crearon y validaron dumps custom de ambas bases. La restauración de la aplicación en `meal_planner_restore_test`, incluida la doble verificación, tardó 6,2 s en Docker Desktop. Se comprobaron 6 usuarios, 24 productos, 10 planes y 10 listas; después se eliminó exclusivamente la base temporal. Es una medición local reproducible, no un SLA.
