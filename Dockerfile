# syntax=docker/dockerfile:1.4



########## RUNTIME ##########
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
ARG SERVICE_PORT=8088
ENV SERVER_PORT=${SERVICE_PORT}
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75" \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE ${SERVICE_PORT}
ENTRYPOINT ["java","-jar","/app/app.jar"]
