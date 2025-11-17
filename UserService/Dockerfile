# STAGE 1: Build the application using Gradle
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app

# Copy only dependency-related files first to leverage Docker cache
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew dependencies --build-cache || ./gradlew dependencies

# Copy the rest of the source code
COPY src ./src

# Build the application
RUN ./gradlew build -x test
RUN for file in /app/build/libs/*.jar; do \
      if ! echo "$file" | grep -q -- "-plain.jar$"; then \
        mv "$file" /app/build/libs/app.jar; \
      fi; \
    done

# STAGE 2: Create the final, lightweight image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/app.jar .

# Expose the application port
EXPOSE 11000

# Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]
