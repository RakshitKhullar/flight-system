# Multi-stage Dockerfile for Flight Booking System
# Stage 1: Build stage
FROM maven:3.9.5-openjdk-17-slim AS builder

# Set working directory
WORKDIR /app

# Copy parent pom.xml first for better layer caching
COPY pom.xml .

# Copy module pom.xml files
COPY reservation-system/pom.xml ./reservation-system/
COPY database-module/pom.xml ./database-module/

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY reservation-system/src ./reservation-system/src
COPY database-module/src ./database-module/src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM openjdk:17-jre-slim AS runtime

# Install curl for health checks and debugging
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/reservation-system/target/*.jar app.jar

# Create logs directory and set permissions
RUN mkdir -p /app/logs && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimization for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=docker"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
