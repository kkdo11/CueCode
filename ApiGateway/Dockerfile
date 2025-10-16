# ================= STAGE 1: Build =================
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# ApiGateway의 소스 코드는 현재 폴더(.)를 기준으로 복사
COPY src ./src
# ApiGateway 모듈의 build.gradle도 복사 (필요한 경우)
COPY build.gradle ./

# --- [핵심 수정] Gradle이 ApiGateway 모듈을 빌드하도록 경로 수정 불필요 ---
# 이제 상위 settings.gradle을 복사했으므로, Gradle이 ':ApiGateway' 모듈을 올바르게 인식함
RUN chmod +x ./gradlew && ./gradlew :ApiGateway:build -x test

# ================= STAGE 2: Final Image =================
FROM openjdk:17-jdk-slim
WORKDIR /app

# [핵심 수정] 빌드된 jar 파일의 정확한 경로를 지정
COPY --from=build /app/ApiGateway/build/libs/*.jar app.jar

EXPOSE 13000
ENTRYPOINT ["java","-jar","/app/app.jar"]