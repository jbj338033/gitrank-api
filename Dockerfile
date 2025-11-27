FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/gitrank-api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=live"]
