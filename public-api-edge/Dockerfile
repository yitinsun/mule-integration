FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/public-api-edge-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
