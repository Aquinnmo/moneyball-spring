FROM gradle:9.4.1-jdk21 AS build
WORKDIR /home/gradle/project

COPY . .
RUN gradle --no-daemon clean bootJar && \
    JAR_FILE="$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)" && \
    if [ -z "$JAR_FILE" ]; then echo "Error: No executable JAR found in build/libs after bootJar" && exit 1; fi && \
    cp "$JAR_FILE" app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /home/gradle/project/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
