# Dockerfile for building MotionService from the root of the monorepo
# Build command: docker build -f MotionService/Dockerfile .

# STAGE 1: Build the application using Gradle
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app

# 1. Copy only the necessary build files from the MotionService sub-directory
COPY MotionService/gradlew .
COPY MotionService/gradle ./gradle
COPY MotionService/build.gradle .
COPY MotionService/settings.gradle .

# 2. Grant execution rights and download dependencies
RUN chmod +x ./gradlew && ./gradlew dependencies

# 3. Copy the application source code
COPY MotionService/src ./src

# 4. Build the application, skipping tests
RUN ./gradlew build -x test

# 5. Find and rename the executable jar for the next stage
RUN find /app/build/libs -name "*.jar" ! -name "*-plain.jar" -exec mv {} /app/build/libs/app.jar \;

# STAGE 2: Create the final, lightweight image
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the built application jar from the build stage
COPY --from=build /app/build/libs/app.jar /app/app.jar

EXPOSE 15000
ENTRYPOINT ["java","-jar","/app/app.jar"]
