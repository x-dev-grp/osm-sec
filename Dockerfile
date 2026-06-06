########## BUILD ##########
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

ARG MAVEN_USERNAME
ARG MAVEN_TOKEN

# 1) write settings.xml with GitHub Packages credentials
RUN --mount=type=cache,id=s/cff44870-2016-419c-b899-f24a73b82b34-/root/.m2,target=/root/.m2 \
    set -eux; \
    mkdir -p /root/.m2; \
    printf '<?xml version="1.0" encoding="UTF-8"?>\n\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">\n\
  <servers>\n\
    <server>\n\
      <id>github</id>\n\
      <username>%s</username>\n\
      <password>%s</password>\n\
    </server>\n\
  </servers>\n\
</settings>\n' "${MAVEN_USERNAME}" "${MAVEN_TOKEN}" > /root/.m2/settings.xml; \
    chmod 600 /root/.m2/settings.xml

# 2) cache deps
COPY pom.xml .
RUN --mount=type=cache,id=s/cff44870-2016-419c-b899-f24a73b82b34-/root/.m2,target=/root/.m2 \
    mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,id=s/cff44870-2016-419c-b899-f24a73b82b34-/root/.m2,target=/root/.m2 \
    mvn -B -DskipTests clean package

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

