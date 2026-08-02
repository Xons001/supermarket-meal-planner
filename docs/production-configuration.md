# Configuración de producción

La configuración compartida vive en `application.yml`; los perfiles `development`, `test` y `production` fijan políticas del entorno. Desarrollo es el perfil predeterminado. Producción exige `SPRING_PROFILES_ACTIVE=production`.

La versión se obtiene del tag `MAJOR.MINOR.PATCH`. Sin tag se usa `0.11.0-dev+<sha>` desde la automatización. `APP_VERSION`, `APP_COMMIT` y `APP_BUILD_TIME` alimentan `/actuator/info`, logs, frontend y labels OCI.

Los secretos obligatorios son access JWT, refresh HMAC, preview HMAC, hash de observabilidad, PostgreSQL y Airflow. Deben ser distintos, externos y no contener placeholders conocidos. Las credenciales nunca deben entrar en argumentos de backup, imágenes, logs o archivos versionados.

Swagger, demo, bootstrap ADMIN, seeds, proveedores mock y nutrición local están deshabilitados en producción. Open Food Facts requiere activación consciente y un User-Agent de contacto identificable.
