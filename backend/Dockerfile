FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/mvnw
COPY backend/mvnw.cmd backend/mvnw.cmd
COPY data/mock data/mock

RUN chmod +x backend/mvnw \
    && cd backend \
    && ./mvnw dependency:go-offline

COPY backend/src backend/src

RUN cd backend \
    && ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

ARG APP_VERSION=0.11.0-dev
ARG VCS_REF=unknown
ARG BUILD_TIME=unknown
LABEL org.opencontainers.image.title="Supermarket Meal Planner Backend" \
      org.opencontainers.image.version="$APP_VERSION" \
      org.opencontainers.image.revision="$VCS_REF" \
      org.opencontainers.image.created="$BUILD_TIME" \
      org.opencontainers.image.licenses="MIT"

RUN apk add --no-cache wget \
    && addgroup -S app \
    && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/backend/target/supermarket-meal-planner-backend-*.jar app.jar

USER app
EXPOSE 8080

ENV APP_VERSION=$APP_VERSION APP_COMMIT=$VCS_REF APP_BUILD_TIME=$BUILD_TIME \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -Djava.io.tmpdir=/tmp"

HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=15 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
