# ── Stage 1: build the jar ──────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first so dependency layer is cached separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: minimal runtime image ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Railway injects PORT; default 8080 for local Docker
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
