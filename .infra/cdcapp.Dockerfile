FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY ../target/blueprint-iceberg-cdc-*.jar app.jar

ENTRYPOINT ["java", \
    "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", \
    "--add-opens=java.base/java.nio=ALL-UNNAMED", \
    "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED", \
    "-jar", "app.jar"]
