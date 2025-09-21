FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/
COPY gradlew ./

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew build -x test

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Change ownership of the app directory
RUN chown -R spring:spring /app
USER spring:spring

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "build/libs/auth-service-0.0.1-SNAPSHOT.jar"]
