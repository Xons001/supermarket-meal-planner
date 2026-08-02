# Roadmap

## FASE 0 — Fundación y arquitectura ✅

Monorepo, monolito modular, PostgreSQL, Flyway, Docker, proveedores
intercambiables, catálogo vertical, frontend inicial, pruebas y documentación.

## FASE 1 — Catálogo completo y frontend ✅

Búsqueda y filtros combinables, alérgenos, etiquetas dietéticas, disponibilidad,
histórico de precios, navegación, paginación, detalle y estados vacíos.

## FASE 2 — Plantillas de comidas ✅

CRUD, ingredientes, unidades compatibles, instrucciones, cálculo nutricional y
de coste consumido, estados parciales, filtros y previsualización.

## FASE 3 — Generador basado en scoring ✅

Motor determinista, preview protegido, snapshots persistidos, listado y detalle.

## FASE 4 — Lista de compra y desperdicio ✅

Paquetes enteros, coste real estimado, cantidad utilizada, sobrante, presupuesto,
avisos, snapshots, filtros, exportación CSV e impresión.

## FASE 5 — Optimización por compra real ✅

Beam Search sensible a envases, coste marginal, desperdicio y reutilización
económicamente útil, manteniendo la estrategia clásica compatible.

## FASE 6 — Edición parcial, sustituciones y bloqueos ✅

Sustitución y regeneración deterministas, bloqueos, previews firmados, versionado
optimista, historial, undo e invalidación lógica de listas.

## FASE 7 — Usuarios, autenticación y propiedad ✅

Cuentas, preferencias, Argon2id, JWT corto, refresh opaco rotatorio, CSRF, CORS,
límites de intentos y aislamiento completo por propietario.

## FASE 8 — Dashboard, organización y experiencia de usuario ✅

Landing pública, navegación contextual, dashboard privado, actividad combinada,
tema, organización de planes y listas, duplicación histórica y E2E aislado.

## FASE 9 — Airflow y sincronización ✅

DAGs idempotentes de catálogo, precios, bajas lógicas, staging, informes y panel ADMIN.

## FASE 10 — Nutrición externa

Evaluación de Open Food Facts, matching y confianza verificable.

## FASE 11 — OR-Tools

Optimización avanzada como estrategia intercambiable.

## FASE 12 — IA opcional

Interpretación de lenguaje natural sin delegar cálculos de negocio.

## FASE 13 — CI/CD, despliegue y Kubernetes

Pipeline, observabilidad, despliegue reproducible y demostración Kubernetes.
