# ADR 0027: Airflow 3 para orquestación

## Decisión
Usar Apache Airflow 3.3 con `LocalExecutor`, PostgreSQL de metadatos independiente y tres DAG pequeños. Evitamos Celery y Redis mientras la carga sea local.
