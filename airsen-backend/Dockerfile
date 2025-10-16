# syntax=docker/dockerfile:1
#
# Optimized Multi-stage Dockerfile for Airsen Backend (Spring Boot)
# Supports: development (with DevTools hot reload), production
#
# Key Optimizations:
# - Dev: No source copy (bind-mounted), only dependencies cached
# - Prod: Clean multi-stage build, minimal runtime image
# - Dependencies cached separately from source code
#

# ==========================================
# DEVELOPMENT STAGE - Hot Reload with DevTools
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS development

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven curl bash

# Copy ONLY pom.xml for dependency caching (source will be bind-mounted)
COPY pom.xml .

# Download dependencies (cached layer - only invalidates if pom.xml changes)
RUN mvn dependency:go-offline -B || true

# DON'T COPY src - will be bind-mounted in docker-compose
# This allows Spring Boot DevTools to detect file changes and auto-restart

# Expose application and debug ports
EXPOSE 8080 5005

# Spring Boot DevTools auto-restart enabled by default
# DevTools will watch for changes in bind-mounted /app/src directory
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-Xms256m -Xmx512m"]

# ==========================================
# BUILDER STAGE - Compile Application
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code (invalidates only if src changes)
COPY src ./src

# Build application JAR (skip tests for faster builds)
RUN mvn clean package -DskipTests -B && \
    mv target/*.jar app.jar

# ==========================================
# PRODUCTION STAGE - Minimal Runtime Image
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS production

WORKDIR /app

# Install only curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Copy ONLY the compiled JAR from builder stage
COPY --from=builder --chown=spring:spring /build/app.jar .

# Health check for production readiness
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose application port
EXPOSE 8080

# JVM optimization flags for production
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run application with optimized JVM settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
