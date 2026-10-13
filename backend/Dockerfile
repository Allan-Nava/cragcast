# Multi-stage build. Both Temurin images are multi-arch, so the same file builds the
# linux/arm64 image the Oracle Ampere VM runs (CI builds it with buildx, CG-9).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 cragcast
COPY --from=build /src/target/cragcast-backend-*.jar app.jar
USER 10001
EXPOSE 8080
# Leave the JVM a share of the container's memory instead of a fixed -Xmx.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
