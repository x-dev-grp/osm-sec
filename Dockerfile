# Stage 1: Build the app using Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine as build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the built JAR
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
