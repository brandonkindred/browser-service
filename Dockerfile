# syntax=docker/dockerfile:1.7
# Multi-stage build for browser-service-api.
# - stage 1: warm the local Maven cache so subsequent builds are incremental.
# - stage 2: compile + package the layered Spring Boot jar.
# - stage 3: extract Spring Boot layers for cache-friendly image composition.
# - stage 4: runtime image based on eclipse-temurin:21-jre.

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

FROM eclipse-temurin:21-jre AS layers
WORKDIR /workspace
COPY --from=build /src/api/target/*-exec.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=layers /workspace/dependencies/           ./
COPY --from=layers /workspace/spring-boot-loader/     ./
COPY --from=layers /workspace/snapshot-dependencies/  ./
COPY --from=layers /workspace/application/            ./
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]
