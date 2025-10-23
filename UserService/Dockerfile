# STAGE 1: Build the application using Gradle with dependency caching
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app

# 1. Copy only dependency-related files first
# [수정] gradlew 실행 파일을 함께 복사합니다.
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 2. Download dependencies. This layer is cached as long as build.gradle doesn't change.
# [수정] gradlew에 실행 권한을 부여합니다.
RUN chmod +x ./gradlew && ./gradlew dependencies

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

# 각 서비스에 맞는 포트 번호로 수정해야 합니다.
EXPOSE 11000
ENTRYPOINT ["java","-jar","/app/app.jar"]