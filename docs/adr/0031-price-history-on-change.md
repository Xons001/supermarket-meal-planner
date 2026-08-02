# ADR 0031: historial solo ante cambios efectivos

## Decisión
Crear historial si cambia precio o precio unitario frente al valor vigente. `sync_run_id` evita duplicados dentro de una ejecución y `source` conserva procedencia.
