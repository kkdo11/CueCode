# STAGE 1: Build the application using Gradle
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app
# Copy the entire project context (respecting .dockerignore)
COPY . .
# Grant execution rights and run the build
RUN chmod +x ./gradlew && ./gradlew build -x test

# STAGE 2: Create the final, lightweight image
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copy only the built jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","/app/app.jar"]