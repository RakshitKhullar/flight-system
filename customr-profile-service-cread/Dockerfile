# Build stage
FROM maven:3.8.6-openjdk-17-slim AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY customer-profile-service/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY customer-profile-service/src ./src
RUN mvn package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]

# Expose the application port
EXPOSE 8090
