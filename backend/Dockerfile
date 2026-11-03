# Multi-stage build for Spring Boot Backend
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and dependency sources first to leverage layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and build package
COPY src ./src
RUN mvn clean package -DskipTests -B

# Final runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built artifact
COPY --from=build /app/target/*.jar app.jar

# Create non-root user for security and assign ownership
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
