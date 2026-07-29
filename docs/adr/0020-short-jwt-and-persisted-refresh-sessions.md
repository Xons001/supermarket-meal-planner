# ADR 0020 — JWT corto y sesiones refresh persistidas

## Estado

Aceptado.

## Decisión

Usar access JWT HS256 de 15 minutos y refresh opaco de 30 días. El JWT identifica
una sesión persistida que se consulta en cada petición. Solo se almacena el HMAC
del refresh.

## Consecuencias

La autorización no requiere almacenar access tokens y logout es inmediato. Hay
una lectura de sesión por petición privada y los secretos access/refresh deben
ser externos, distintos y de al menos 32 bytes.
