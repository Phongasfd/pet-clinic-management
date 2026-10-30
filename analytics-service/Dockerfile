# Built from the repository root (see docker-compose*.yml): proto-common is a local
# Maven module, not published anywhere, so it must be installed before this service builds.
# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY proto-common/ proto-common/
RUN mvn -B -q -f proto-common/pom.xml install -DskipTests

# Cache dependencies before copying the source
COPY analytics-service/pom.xml analytics-service/
RUN mvn -B -f analytics-service/pom.xml dependency:go-offline

COPY analytics-service/src analytics-service/src
RUN mvn -B -f analytics-service/pom.xml package -DskipTests

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/analytics-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
