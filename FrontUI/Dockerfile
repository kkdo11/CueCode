FROM eclipse-temurin:17-jre
ENV TZ=Asia/Seoul
WORKDIR /app

COPY build/libs/*.jar /app/app.jar

# 각 서비스에 맞는 포트 노출 (예: FrontUI는 8080)
EXPOSE 14000

ENTRYPOINT ["java","-jar","/app/app.jar"]