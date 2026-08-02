# Privacidad

La aplicación almacena cuenta, preferencias, planes, listas y actividad para ofrecer planificación privada y trazabilidad. Minimiza sesiones a hashes y excluye secretos de logs y exportaciones.

`GET /api/v1/users/me/export` descarga JSON propio con `no-store`, propiedad validada y límite de dos exportaciones por hora. No incluye contraseñas, hashes, sesiones, tokens, auditoría interna ni datos ajenos.

La desactivación es lógica y conserva recursos para integridad histórica. Datos de negocio y actividad se conservan indefinidamente por ahora; backups expiran a 14 días. Esta descripción es informativa y no afirma certificación ni cumplimiento jurídico.
