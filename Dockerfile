# ETAPA 1: Compilar con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache de dependencias (si pom.xml no cambia, no re-descarga)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Compilar el proyecto
COPY src ./src
RUN mvn package -DskipTests -q

# ETAPA 2: Solo el jar en imagen ligera
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]