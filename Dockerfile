# Build backend when Railway (or Docker) uses the repo root as context.
# If your Railway service has Root Directory = "backend", it will use backend/Dockerfile instead.
# ── Stage 1: build the jar ──────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY backend/pom.xml .
RUN mvn dependency:go-offline -q

COPY backend/src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: minimal runtime image ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
