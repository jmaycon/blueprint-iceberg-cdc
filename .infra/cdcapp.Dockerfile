FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY ../target/blueprint-iceberg-cdc-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
