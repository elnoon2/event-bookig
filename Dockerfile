# =========================================================================
#  Dockerfile for Railway deployment (Badya University Event Booking)
#  Builds the Spring Boot backend AND serves the static frontend together
#  from a single container on one URL.
# =========================================================================

# ---- Stage 1: Build the Spring Boot jar with Maven (Java 17) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the backend Maven project and build the jar
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: Lightweight runtime image ----
FROM eclipse-temurin:17-jre
# The app runs from /app/backend so that Spring's WebConfig (which serves the
# frontend from the parent directory "..") finds the HTML/CSS/JS in /app.
WORKDIR /app/backend

# The built application jar
COPY --from=build /build/target/*.jar /app/backend/app.jar

# Frontend assets -> /app (parent of the working directory)
COPY index.html admin.html scan.html profile.html /app/
COPY script.js admin.js scan.js admin-config.js /app/
COPY styles.css admin.css scan.css /app/
COPY images /app/images
COPY models /app/models

# Writable uploads directory (relative to the working directory /app/backend)
RUN mkdir -p /app/backend/uploads

# Run with the production profile (MySQL) by default
ENV SPRING_PROFILES_ACTIVE=prod

# Railway injects the PORT env var; the app reads it (see application.properties)
EXPOSE 5000

CMD ["java", "-jar", "/app/backend/app.jar"]
