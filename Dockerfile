FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY target/middleware-backend-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
