# ADR 0024 — Rate limiting de identidad en memoria

## Estado

Aceptado para el despliegue monoinstancia actual.

## Decisión

Limitar login, registro, refresh y cambio de contraseña con ventanas basadas en
`Clock` y fingerprints SHA-256, sin guardar IP o correo en claro.

## Consecuencias

Es sencillo, determinista y testeable, pero no coordina réplicas y se reinicia
con el proceso. Antes de escalar horizontalmente deberá migrarse a un almacén
distribuido.
