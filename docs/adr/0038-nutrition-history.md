# ADR 0038 — Historial nutricional append-only

Cada cambio efectivo conserva snapshot anterior/nuevo, fuente, confianza, actor, motivo y fecha. Un hash único por producto hace idempotentes reintentos y evita historial ficticio.
