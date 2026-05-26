# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Cache Gradle dependency layer (only re-downloaded on build.gradle change)
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

# Copy source and build the fat JAR
COPY src/ src/
COPY models/ models/
RUN ./gradlew bootJar --no-daemon -x test -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
LABEL org.opencontainers.image.title="rootcause-suggester" \
      org.opencontainers.image.description="AI-Assisted CI-Failure Root-Cause Suggester" \
      org.opencontainers.image.version="1.0.0"

WORKDIR /app

# Non-root user (using Debian commands)
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# App artefacts
COPY --from=build /app/build/libs/rootcause-suggester-*.jar app.jar
COPY --from=build /app/models /app/models
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

# Liveness probe
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
  # Container-aware heap sizing (use 75 % of cgroup memory limit)
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  # G1GC for low-latency responses
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  # Always dump heap on OOM so we can post-mortem
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/tmp/heap.hprof", \
  # Open modules needed by Hibernate / ONNX runtime
  "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
  "--add-opens", "java.base/java.util=ALL-UNNAMED", \
  "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", \
  "-jar", "app.jar"]

