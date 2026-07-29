# ADR 0023 — Recursos por propietario y migración histórica

## Estado

Aceptado.

## Decisión

Planes y listas tienen `owner_id NOT NULL`; una FK compuesta garantiza
copropiedad. Todos los accesos privados consultan `id + ownerId` y los recursos
ajenos responden 404. V10 asigna históricos a una cuenta técnica deshabilitada.

## Consecuencias

No se filtra la existencia de UUID ajenos. La demo opcional solo puede adoptar
recursos que sigan bajo el propietario técnico.
