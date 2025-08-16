# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /app/target/photo-porter-*.jar app.jar

RUN addgroup --system photo-porter && \
    adduser --system --ingroup photo-porter photo-porter && \
    chown -R photo-porter:photo-porter /app

USER photo-porter

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
