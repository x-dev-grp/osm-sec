# syntax=docker/dockerfile:1.4

########## BUILD ##########
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# 1) cache deps
COPY pom.xml .
RUN --mount=type=secret,id=maven_settings,required \
    --mount=type=cache,target=/root/.m2 \
    set -eux; \
    mkdir -p /root/.m2; \
    install -m 600 /run/secrets/maven_settings /root/.m2/settings.xml; \
    mvn -B -s /root/.m2/settings.xml -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=secret,id=maven_settings,required \
    --mount=type=cache,target=/root/.m2 \
    set -eux; \
    mkdir -p /root/.m2; \
    install -m 600 /run/secrets/maven_settings /root/.m2/settings.xml; \
    mvn -B -s /root/.m2/settings.xml -DskipTests clean package

########## RUNTIME ##########
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 -g root appuser

COPY --from=build /app/target/*.jar /app/app.jar
ARG SERVICE_PORT=8088
ENV SERVER_PORT=${SERVICE_PORT}
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75" \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE ${SERVICE_PORT}
USER appuser
ENTRYPOINT ["java","-jar","/app/app.jar"]
