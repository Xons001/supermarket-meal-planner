# Roadmap

## FASE 0 — Fundación y arquitectura ✅

Monorepo, monolito modular, PostgreSQL, Flyway, Docker, proveedores
intercambiables, catálogo vertical, frontend inicial, pruebas y documentación.

## FASE 1 — Catálogo completo y frontend ✅

Búsqueda y filtros combinables, alérgenos, etiquetas dietéticas, disponibilidad,
histórico de precios, navegación, paginación, detalle, estados vacíos y filtros
persistidos en la URL.

## FASE 2 — Plantillas de comidas ✅

CRUD y archivado lógico, ingredientes del catálogo, unidades compatibles,
instrucciones ordenadas, cálculo nutricional y de coste consumido, estados
parciales, filtros, previsualización, frontend responsive y pruebas integrales.

## FASE 3 — Generador basado en scoring ✅

Motor determinista para calorías, proteína, presupuesto y variedad; preview
protegido por token, snapshots persistidos, listado, detalle y archivado lógico.

## FASE 4 — Lista de compra y desperdicio ✅

Agregación global del plan, paquetes enteros, coste real estimado de compra,
cantidad utilizada y sobrante, presupuesto, avisos, snapshots, filtros,
exportación CSV e impresión.

## FASE 5 — Optimización por compra real ✅

Beam Search sensible a envases completos, coste marginal, desperdicio y
reutilización económicamente útil. Incluye estrategia clásica compatible,
presets controlados, snapshots y consistencia con listas de compra.

## FASE 6 — Edición parcial, sustituciones y bloqueos ✅

Sustitución y regeneración deterministas de comidas o días, bloqueos,
previews firmados, versionado optimista, historial, undo e invalidación lógica
de listas de compra.

## FASE 7 — Usuarios, autenticación y propiedad ✅

Cuentas y preferencias, Argon2id, JWT corto en cookie HttpOnly, refresh opaco
rotatorio, CSRF, CORS, límites de intentos y aislamiento completo por propietario.

## FASE 8 — Airflow y sincronización

DAGs idempotentes de catálogo, precios, bajas lógicas e informes.

## FASE 9 — Nutrición externa

Evaluación de Open Food Facts, matching y confianza verificable.

## FASE 10 — OR-Tools

Optimización avanzada como estrategia intercambiable.

## FASE 11 — IA opcional

Interpretación de lenguaje natural sin delegar cálculos de negocio.

## FASE 12 — CI/CD, despliegue y Kubernetes

Pipeline, observabilidad, despliegue reproducible y demostración Kubernetes.
