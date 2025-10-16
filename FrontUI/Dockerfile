# STAGE 1: Build the application using Gradle
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew build -x test
# Find the executable jar and rename it to a predictable name
RUN find /app/build/libs -name "*.jar" ! -name "*-plain.jar" -exec mv {} /app/build/libs/app.jar \;

# STAGE 2: Create the final, lightweight image
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copy the renamed jar from the build stage
COPY --from=build /app/build/libs/app.jar /app/app.jar
EXPOSE 14000
ENTRYPOINT ["java","-jar","/app/app.jar"]
