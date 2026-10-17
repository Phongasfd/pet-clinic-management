# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache dependencies trước khi copy source
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Build JAR (skip tests để build nhanh hơn)
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# HTTP port
EXPOSE 8083
# gRPC port
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
