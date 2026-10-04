# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B package -DskipTests

# ---- Run stage ----
# Needs the full JDK (javac, not just the JRE) plus g++/python3/node -- CodeRunnerService
# shells out to all four to compile/run user submissions in Java, C++, Python, and JavaScript.
FROM eclipse-temurin:17-jdk
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends g++ python3 nodejs \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
