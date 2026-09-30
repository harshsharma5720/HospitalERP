# syntax=docker/dockerfile:1
# Backend image: builds all Maven modules, runs the one executable jar (hospital-app).
# The modules (common, identity, patients, ...) are libraries inside that jar, not separate services.
# Build context: hospitalERP/   (docker compose does this; by hand: docker build -t hospital-backend hospitalERP)

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
# Tests run in CI / locally (the MySQL migration test needs Docker itself); the image only packages.
# The cache mount keeps downloaded dependencies between builds.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 hospital && mkdir -p /app/uploads && chown hospital /app/uploads
COPY --from=build /build/app/target/hospital-app-*.jar /app/app.jar
USER hospital
# Profile images are stored in /app/uploads (mount a volume there to keep them)
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
