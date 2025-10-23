# STAGE 1: Build the application using Gradle with dependency caching
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app

# 1. Copy only dependency-related files first
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 2. Download dependencies. This layer is cached as long as build.gradle doesn't change.
RUN ./gradlew dependencies

# 3. Copy the rest of the source code
COPY src ./src

# 4. Build the application (dependencies are already cached)
RUN ./gradlew build -x test

# Find the executable jar and rename it to a predictable name
RUN find /app/build/libs -name "*.jar" ! -name "*-plain.jar" -exec mv {} /app/build/libs/app.jar \;

# STAGE 2: Create the final, lightweight image
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the renamed jar from the build stage
COPY --from=build /app/build/libs/app.jar /app/app.jar

EXPOSE 13000
ENTRYPOINT ["java","-jar","/app/app.jar"]
