FROM nginxinc/nginx-unprivileged:1.29-alpine
ARG APP_VERSION=0.11.0-dev
ARG VCS_REF=unknown
LABEL org.opencontainers.image.title="Supermarket Meal Planner Edge" \
      org.opencontainers.image.version="$APP_VERSION" \
      org.opencontainers.image.revision="$VCS_REF" \
      org.opencontainers.image.licenses="MIT"
COPY infrastructure/nginx/production.conf /etc/nginx/conf.d/default.conf
USER nginx
HEALTHCHECK --interval=10s --timeout=5s --retries=10 CMD wget --no-check-certificate --quiet --spider https://127.0.0.1:8443/healthz || exit 1
