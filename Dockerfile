# Local MVP image — Java 25. Prefer building with Corretto/Temurin 25.
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S logdetective && adduser -S logdetective -G logdetective
COPY --from=build /workspace/target/sherlock-*.jar /app/app.jar
USER logdetective
EXPOSE 8080
ENV SERVER_ADDRESS=127.0.0.1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
