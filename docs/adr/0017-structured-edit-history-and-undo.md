# ADR 0017 — Historial estructurado y undo

## Decisión

Persistir cada cambio en `meal_plan_changes` con IDs, snapshots, métricas,
versiones, seed, estrategia y estado de undo. Undo solo restaura el último
cambio de contenido no deshecho y conserva bloqueos actuales.

## Consecuencias

La auditoría no depende de mensajes de texto y una operación con varias comidas
se restaura atómicamente.
