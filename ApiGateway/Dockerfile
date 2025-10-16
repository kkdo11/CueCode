# ================= STAGE 1: Build =================
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app

COPY ApiGateway/gradlew .
COPY ApiGateway/gradle ./gradle
COPY ApiGateway/build.gradle .
COPY ApiGateway/settings.gradle .
COPY ApiGateway/src ./src

RUN chmod +x ./gradlew && ./gradlew build -x test

# ================= STAGE 2: Final Image =================
FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 13000
ENTRYPOINT ["java","-jar","/app/app.jar"]