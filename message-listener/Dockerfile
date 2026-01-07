# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# SQLite directory setup
RUN mkdir ./data && chown -R 1000:1000 ./data

# Copy the specifically named JAR
COPY --from=build /app/target/weather-app.jar weather-app.jar

USER 1000

# Use ENTRYPOINT for the fixed executable
ENTRYPOINT ["java", "-Xms64m", "-Xmx128m", "-XX:+UseSerialGC", "-jar", "weather-app.jar"]

# Use CMD for default arguments (can be empty or used for Spring profiles)
CMD []