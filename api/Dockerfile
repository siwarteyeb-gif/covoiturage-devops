
# syntax=docker/dockerfile:1

# ---- Etape 1 : compilation ----
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B package -Dmaven.test.skip=true

# ---- Etape 2 : execution ----
FROM eclipse-temurin:17.0.13_11-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD wget -qO /dev/null http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
