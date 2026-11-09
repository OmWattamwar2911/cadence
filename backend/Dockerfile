# Stage 1: Build JAR using Maven
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cap Maven memory for cloud builders with limited RAM (e.g. Render 512MB)
ENV MAVEN_OPTS="-Xmx384m -XX:+UseSerialGC"

# Copy pom.xml and source files
COPY pom.xml .
COPY src ./src

# Build production jar skipping tests
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight, secure JRE runtime
FROM eclipse-temurin:21-jre-jammy

# Install curl for healthchecks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root system user and group
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/sh -m appuser

WORKDIR /app

# Copy artifact from builder stage with correct ownership
COPY --from=builder --chown=appuser:appgroup /build/target/wallet-service-backend-1.0.0.jar app.jar

# Run as non-root user
USER appuser

EXPOSE 8080 10000

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
