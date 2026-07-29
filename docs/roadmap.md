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

## FASE 6 — Airflow y sincronización

DAGs idempotentes de catálogo, precios, bajas lógicas e informes.

## FASE 7 — Nutrición externa

Evaluación de Open Food Facts, matching y confianza verificable.

## FASE 8 — Usuarios y preferencias

Autenticación, perfiles, favoritos, historial y planes guardados.

## FASE 9 — OR-Tools

Optimización avanzada como estrategia intercambiable.

## FASE 10 — IA opcional

Interpretación de lenguaje natural sin delegar cálculos de negocio.

## FASE 11 — CI/CD, despliegue y Kubernetes

Pipeline, observabilidad, despliegue reproducible y demostración Kubernetes.
