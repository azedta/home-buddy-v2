# =========================
# 1) Build stage
# =========================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only pom first for dependency caching
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -q -DskipTests clean package

# =========================
# 2) Run stage
# =========================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Render uses PORT env var; Spring should bind to it
ENV PORT=8080
EXPOSE 8080

# Force Spring to use Render's port
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
