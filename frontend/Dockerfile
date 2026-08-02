FROM node:22.22.0-alpine AS dependencies

WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

FROM dependencies AS source

COPY frontend/ ./

FROM source AS test

CMD ["sh", "-c", "npm run format:check && npm run lint && npm run test && npm run build"]

FROM source AS build

ARG APP_VERSION=0.11.0-dev
ARG VCS_REF=unknown
ARG PUBLIC_BASE_URL=
ENV VITE_APP_VERSION=$APP_VERSION VITE_APP_COMMIT=$VCS_REF VITE_PUBLIC_BASE_URL=$PUBLIC_BASE_URL
RUN npm run build

FROM nginx:1.29-alpine AS runtime

ARG APP_VERSION=0.11.0-dev
ARG VCS_REF=unknown
ARG BUILD_TIME=unknown
LABEL org.opencontainers.image.title="Supermarket Meal Planner Frontend" \
      org.opencontainers.image.version="$APP_VERSION" \
      org.opencontainers.image.revision="$VCS_REF" \
      org.opencontainers.image.created="$BUILD_TIME" \
      org.opencontainers.image.licenses="MIT"

COPY frontend/nginx-main.conf /etc/nginx/nginx.conf
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY frontend/security-headers.conf /etc/nginx/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html

USER nginx
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=5s --retries=10 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/ || exit 1
