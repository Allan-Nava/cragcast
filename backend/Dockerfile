# Multi-stage build, run from the repository root. The jar includes the data files the pom
# lists (crags.json, avalanche-regions.geojson, osm-climbing.json): each must be copied below,
# or the image lacks it while the build from the repository, and so CI, has it.
#   docker build -f backend/Dockerfile .
# Both Temurin images are multi-arch: the same file builds the linux/arm64 image of the
# Oracle Ampere VM (image.yml).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY data/crags.json data/avalanche-regions.geojson data/osm-climbing.json /src/data/
COPY backend/src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# The release this image is (vYYYY.MM.DD, from release.yml), read by /actuator/info; dev otherwise.
ARG VERSION=dev
ENV CRAGCAST_VERSION=$VERSION
RUN useradd --system --uid 10001 cragcast
COPY --from=build /src/backend/target/cragcast-backend-*.jar app.jar
USER 10001
EXPOSE 8080
# Leave the JVM a share of the container's memory instead of a fixed -Xmx.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
