# ADR 0029: idempotencia por run e identidad externa

## Decisión
Combinar unicidad por supermercado/identificador externo, run de sincronización y hashes canónicos. Un reintento crea una ejecución nueva y nunca reabre la anterior.
