ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre-jammy

# DEVELOPMENT STAGE
FROM ${MAVEN_IMAGE} AS development
RUN useradd -ms /bin/bash spring
USER spring
WORKDIR /app
ENV HOME=/home/spring
ENV MAVEN_CONFIG=/home/spring/.m2
COPY --chown=spring:spring pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
RUN mkdir -p /app/src
EXPOSE 8080
CMD ["mvn","spring-boot:run","-Dspring-boot.run.jvmArguments=-Xms256m -Xmx512m"]

# BUILD STAGE
FROM ${MAVEN_IMAGE} AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package
# Verify single JAR output
RUN test $(ls -1 target/*.jar | wc -l) -eq 1

# PRODUCTION STAGE
FROM ${JRE_IMAGE} AS production
WORKDIR /app

# Create non-root user
RUN useradd -ms /bin/bash spring

# Copy JAR with explicit naming
COPY --from=build --chown=spring:spring /app/target/*.jar /app/app.jar

USER spring

# Health check without curl (uses Java)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD java -cp /app/app.jar org.springframework.boot.loader.JarLauncher --spring.main.web-environment=false --management.health.defaults.enabled=false || exit 1

EXPOSE 8080

# Production JVM flags
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/heapdump.hprof", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]