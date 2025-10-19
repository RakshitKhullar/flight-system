# Multi-stage build for Spring Boot application
FROM openjdk:17-jdk-slim as builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY database-module/pom.xml ./database-module/
COPY travel-search-service/pom.xml ./travel-search-service/

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY database-module/src ./database-module/src/
COPY travel-search-service/src ./travel-search-service/src/

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM openjdk:17-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/travel-search-service/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
