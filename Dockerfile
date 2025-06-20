# syntax=docker/dockerfile:1.4        ## ← enable BuildKit secrets & cache

############################
# ⬆️ 1. BUILD STAGE (Maven)
############################
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# --- 1. copy POM & pre-fetch deps (better cache hits) ---
COPY pom.xml .
RUN --mount=type=secret,id=maven_settings,dst=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    mvn -B -s /root/.m2/settings.xml dependency:go-offline

# --- 2. copy the rest of the sources & compile ---
COPY src src
RUN --mount=type=secret,id=maven_settings,dst=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    mvn -B -s /root/.m2/settings.xml clean package -DskipTests

############################
# ⬇️ 2. RUNTIME STAGE (JRE)
############################
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# copy the fat JAR produced in the build stage
COPY --from=build /app/target/*.jar app.jar

# tunable memory limits in containers
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8084
ENTRYPOINT ["java","-jar","app.jar"]
