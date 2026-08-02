# ADR 0033: disparo asíncrono por REST

## Decisión
El backend persiste primero un run y solicita después el DAG mediante la API v2 y JWT de Airflow. Devuelve `202`; los fallos quedan persistidos como `FAILED` con Problem Details estable.
