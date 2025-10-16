# ================= STAGE 1: Build =================
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app

COPY ApiGateway/gradlew .
COPY ApiGateway/gradle ./gradle
COPY ApiGateway/gradle/wrapper/gradle-wrapper.properties ./gradle/wrapper/
COPY ApiGateway/build.gradle .
COPY ApiGateway/settings.gradle .
COPY ApiGateway/src ./src

# 디버깅을 위한 파일 목록 출력
RUN ls -lR

RUN chmod +x ./gradlew && ./gradlew build -x test

# ================= STAGE 2: Final Image =================
FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 13000
ENTRYPOINT ["java","-jar","/app/app.jar"]