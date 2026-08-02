# Releases y rollback

Los tags siguen SemVer `MAJOR.MINOR.PATCH`. La publicación GHCR permanece desactivada hasta definir `PUBLISH_GHCR=true` y aprobar permisos. Cada release conserva SBOMs y resultados de smoke.

Las migraciones son forward-only. El despliegue ejecuta smoke sobre landing, login, health y flujos privados antes de promoverse. Para rollback se vuelve a la imagen anterior solo si es compatible con el esquema ya migrado. Si no lo es, se despliega una corrección forward; restaurar base es la última opción y requiere un backup previo verificado.
