# Stage 1: Build with Maven
FROM maven:3.8.6-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM openjdk:17-jre-slim
WORKDIR /app

# Create directory for static images
RUN mkdir -p /app/static/images

# Copy the packaged JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Launch the application
ENTRYPOINT ["java", "-jar", "app.jar"]