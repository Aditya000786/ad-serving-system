# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY ad-server-core/pom.xml ad-server-core/
COPY ad-server-api/pom.xml ad-server-api/
COPY targeting-engine/pom.xml targeting-engine/
COPY budget-manager/pom.xml budget-manager/
COPY event-processor/pom.xml event-processor/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/ad-server-api/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
