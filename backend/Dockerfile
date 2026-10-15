# Multi-stage build, run from the repository root (the jar includes data/crags.json):
#   docker build -f backend/Dockerfile .
# Both Temurin images are multi-arch: the same file builds the linux/arm64 image of the
# Oracle Ampere VM (image.yml).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY data/crags.json /src/data/crags.json
COPY backend/src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 cragcast
COPY --from=build /src/backend/target/cragcast-backend-*.jar app.jar
USER 10001
EXPOSE 8080
# Leave the JVM a share of the container's memory instead of a fixed -Xmx.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
