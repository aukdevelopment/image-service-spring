# Stage 1: Build with Maven
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create directory for static images
RUN mkdir -p /app/static/images

# Copy the packaged JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Launch the application
ENTRYPOINT ["java", "-jar", "app.jar"]