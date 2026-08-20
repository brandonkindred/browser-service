# syntax=docker/dockerfile:1.7
# Multi-stage build for browser-service-api (Quarkus 3 fast-jar).
# - stage 1: warm the local Maven cache so subsequent builds are incremental.
# - stage 2: compile + package the Quarkus fast-jar.
# - stage 3: runtime image based on eclipse-temurin:21-jre.

FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /src
COPY pom.xml .
COPY engine/pom.xml engine/pom.xml
COPY api/pom.xml api/pom.xml
RUN mvn -q -pl engine,api -am -DskipTests dependency:go-offline

FROM deps AS build
COPY engine engine
COPY api api
RUN mvn -q -pl api -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/api/target/quarkus-app/ ./quarkus-app/
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "quarkus-app/quarkus-run.jar"]
