# Contribuir

1. Crea una rama desde `main` y limita el cambio a un objetivo.
2. No añadas secretos, datos personales ni credenciales a fixtures o logs.
3. Ejecuta `make test`, `docker compose config` y `git diff --check`.
4. Abre una PR explicando alcance, pruebas, migraciones y riesgos.

Las migraciones Flyway son forward-only: nunca se modifica una migración aplicada ni se ejecuta `flyway clean`. Los cambios mayores de dependencias se revisan individualmente.

Al participar aceptas el [código de conducta](CODE_OF_CONDUCT.md).
