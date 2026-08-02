# Protección de rama

Configuración recomendada para `main`: PR obligatorio, checks Backend/Frontend/Airflow/E2E/Security/Docker, una revisión, conversaciones resueltas y bloqueo de force-push y borrado. Los commits firmados son opcionales.

Dependabot se ejecuta semanalmente para Maven, npm, pip, Docker y Actions; agrupa minor/patch. Los cambios major nunca se fusionan automáticamente. Las excepciones de seguridad siguen la política descrita en `SECURITY.md`.
