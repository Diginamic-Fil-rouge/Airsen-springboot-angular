# Multi-stage build for Java 21
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn clean package -DskipTests

# Development stage - for local development with hot reload
FROM maven:3.9-eclipse-temurin-21-alpine AS development

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user with home directory and Maven cache directory
RUN addgroup -S airsen && adduser -S airsen -G airsen -h /home/airsen && \
    mkdir -p /home/airsen/.m2 && \
    mkdir -p /app/target && \
    chown -R airsen:airsen /home/airsen /app

# Copy source code (will be overridden by volume mount in docker-compose)
COPY . .

# Download dependencies for faster startup
RUN mvn dependency:go-offline -B

# Change ownership after copying
RUN chown -R airsen:airsen /app

USER airsen

# Development server with hot reload
EXPOSE 8080
CMD ["mvn","clean","spring-boot:run"]

# Production stage
FROM eclipse-temurin:21-jdk-alpine AS production

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S airsen && adduser -S airsen -G airsen

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown airsen:airsen app.jar

USER airsen

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]