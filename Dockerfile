# Etapa 1: compila o projeto com Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Etapa 2: imagem final só com o Java e o .jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/checkpoint-back-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
