# ADR 0021 — Cookies HttpOnly, CSRF y CORS

## Estado

Aceptado.

## Decisión

Transportar access y refresh en cookies HttpOnly y proteger todos los métodos
mutables con double-submit CSRF. CORS permite credenciales solo a orígenes
configurados. `SameSite=None` exige `Secure=true`.

## Consecuencias

La SPA no maneja tokens de sesión. Debe obtener `/auth/csrf`, enviar
`X-XSRF-TOKEN` y usar `credentials: include`.
