# Case study — Supermarket Meal Planner

## Problema

Traducir objetivos nutricionales y presupuesto a un plan comprable exige trabajar con envases completos, sobrantes, disponibilidad y datos históricos, no solo sumar porciones ideales.

## Solución

Un monolito modular Spring Boot mantiene catálogo, plantillas, generación determinista, listas, identidad y administración. React ofrece la experiencia pública/privada. Airflow separa sincronización y enriquecimiento mediante staging e idempotencia. PostgreSQL y snapshots preservan explicabilidad.

```mermaid
flowchart LR
  U[Browser] --> E[Nginx HTTPS]
  E --> F[React]
  E --> B[Spring Boot]
  B --> P[(PostgreSQL)]
  A[Airflow] --> P
  M[Prometheus] --> B
  G[Grafana] --> M
```

## Decisiones destacadas

- JWT breve en cookie HttpOnly, refresh opaco rotatorio y CSRF.
- Scoring clásico compatible y optimización purchase-aware explicable.
- Snapshots y versionado para edición, undo, duplicación y freshness.
- Proveedores externos aislados; producción deshabilita mocks por defecto.
- Compose productivo de una máquina con redes privadas y observabilidad opcional.

## Evidencia

La verificaciÃ³n local de FASE 11 produjo resultados reproducibles: 102 pruebas
backend en 147 s, 57 pruebas frontend, 9 pruebas Python y 17 escenarios
Playwright en 390, 768, 1024 y 1440 px. El bundle inicial medido fue 347,88 kB
(103,94 kB gzip). El smoke de producciÃ³n con TLS autofirmado completÃ³ `/healthz`
y la landing en 38,5 s, incluyendo arranque y limpieza del entorno aislado.

El backup custom-format de ambas bases se verificÃ³ con `pg_restore --list` y se
restaurÃ³ en una base temporal: 6 usuarios, 24 productos, 10 planes, 10 listas y
12 ejecuciones Airflow, sin modificar la base principal. La operaciÃ³n local
combinada tardÃ³ 6,2 s; es una mediciÃ³n, no una garantÃ­a de RTO.

![Landing pÃºblica](images/landing.webp)
![Dashboard privado](images/dashboard.webp)
![Generador semanal](images/generator.webp)
![Detalle del plan](images/plan.webp)
![Lista de compra](images/shopping-list.webp)

No se publican cifras para el benchmark de 100/1.000 productos mientras no se
ejecute en hardware de referencia; los workflows conservan resultados reales y
no convierten objetivos en afirmaciones.

Los resultados de pruebas, tamaños, tiempos, memoria e imágenes se documentan únicamente después de ejecutar los comandos reproducibles. Este documento no inventa benchmarks; consulta el informe de verificación de la fase para valores medidos.
