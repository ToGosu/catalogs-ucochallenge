# Dockerfile para Catalog Service con OpenTelemetry
# Basado en templates/Dockerfile-template

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos Maven (incluyendo wrapper)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Hacer mvnw ejecutable
RUN chmod +x mvnw

# Descargar dependencias (layer caching)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar aplicación
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Instalar wget para health checks
RUN apk add --no-cache wget

# Descargar OpenTelemetry Java Agent
ENV OTEL_AGENT_VERSION=2.4.0
RUN wget -O opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar && \
    chmod 644 opentelemetry-javaagent.jar

# Crear usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar JAR desde builder
COPY --from=builder /app/target/*.jar app.jar

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=docker \
    OTEL_SERVICE_NAME=catalog-service \
    OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317 \
    OTEL_EXPORTER_OTLP_PROTOCOL=grpc \
    OTEL_RESOURCE_ATTRIBUTES=service.name=catalog-service,service.version=0.0.1-SNAPSHOT

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Exponer puerto del servicio
EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:opentelemetry-javaagent.jar -jar app.jar"]

