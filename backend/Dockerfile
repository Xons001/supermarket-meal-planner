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

RUN apk add --no-cache wget \
    && addgroup -S app \
    && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/backend/target/supermarket-meal-planner-backend-*.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
