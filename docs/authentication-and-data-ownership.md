# Autenticación y propiedad de datos

## Modelo de sesión

La contraseña se valida entre 10 y 128 puntos de código Unicode, sin recortarla
ni imponer reglas artificiales de composición, y se almacena con Argon2id. Los
parámetros de producción son configurables; los tests reducen explícitamente el
coste para no ocultar una rebaja accidental en runtime.

Al registrarse o iniciar sesión se emiten:

- `SMP_ACCESS`: JWT HS256 HttpOnly, ruta `/`, duración inicial 15 minutos.
- `SMP_REFRESH`: valor opaco aleatorio HttpOnly, ruta `/api/v1/auth`, duración
  inicial 30 días.
- `XSRF-TOKEN`: token CSRF legible por la SPA y enviado como `X-XSRF-TOKEN`.

El JWT incluye `sub`, rol, `sid`, issuer, emisión y expiración. La firma no basta:
cada petición comprueba que la cuenta continúa activa y que la sesión `sid`
existe, no ha caducado y no está revocada. Por ello el logout es inmediato.

El refresh se rota siempre. En PostgreSQL solo se guarda HMAC-SHA256 del valor
opaco. Reutilizar un token reemplazado revoca su familia completa y responde
`REFRESH_TOKEN_REUSED`. El cambio de contraseña y `logout-all` revocan todas las
sesiones.

## Propiedad y privacidad

`meal_plans.owner_id` y `shopping_lists.owner_id` son obligatorios. Una FK
compuesta `(meal_plan_id, owner_id)` impide incluso a nivel SQL asociar una lista
al plan de otro usuario. Días, comidas y cambios derivan la propiedad del plan.

Un único `CurrentUserProvider` identifica la cuenta. Los repositorios privados
consultan por `id + ownerId`; un UUID válido de otra cuenta se presenta como
`404 RESOURCE_NOT_FOUND`, nunca como `403`, para no confirmar su existencia.
El propietario también forma parte del generation token y del preview HMAC de
edición.

V10 crea un propietario técnico `DISABLED`, sin contraseña utilizable, para
migrar datos históricos. Si la demo se habilita explícitamente, solo adopta
recursos que todavía pertenezcan a ese propietario. Reiniciar no cambia hashes
de cuentas demo o admin existentes.

## Preferencias y precedencia

La petición HTTP de generación admite omitir calorías, proteína, presupuesto,
días, comidas, estrategia, preset, restricciones y alérgenos. La precedencia es:

1. valor explícito de la petición;
2. preferencia del usuario;
3. valores iniciales: 2000 kcal, 100 g, 70 €, 7 días, 4 comidas,
   `PURCHASE_AWARE_SCORING/BALANCED` y listas vacías.

Un plan usado como origen en la SPA prevalece sobre las preferencias.

## CSRF, CORS y cookies

Todos los métodos mutables, incluidos registro, login, refresh y logout,
requieren CSRF. `GET /api/v1/auth/csrf` fuerza la emisión inicial. CORS solo
acepta orígenes configurados y credenciales. `SameSite=None` no arranca si
`Secure=false`. Las respuestas de identidad llevan `Cache-Control: no-store`.

## Límites y auditoría

Los límites en memoria usan `Clock` y claves SHA-256 truncadas no reversibles:
login 5/min por IP+correo, registro 3/h por IP, refresh 30/min por sesión+IP y
cambio de contraseña 5/h por usuario. Al superarlos se devuelve
`429 RATE_LIMIT_EXCEEDED`. En un despliegue con varias réplicas deberá
sustituirse por un almacenamiento distribuido.

La auditoría registra el tipo de evento e IDs o fingerprints, nunca correo
completo, contraseña, cookie, secreto, JWT ni refresh token.

## Configuración obligatoria

`APP_AUTH_ACCESS_TOKEN_SECRET` y `APP_AUTH_REFRESH_TOKEN_SECRET` deben ser
distintos y contener al menos 32 bytes. Si falta alguno, es corto, coincide con
el otro o la política SameSite es insegura, Spring falla al arrancar. Demo y
ADMIN están deshabilitados salvo configuración externa expresa.
