FROM eclipse-temurin:17-jre
ENV TZ=Asia/Seoul
WORKDIR /app

# [수정] .jar 파일 이름을 직접 명시합니다! (ARG 사용 안 함)
COPY build/libs/SpringConfigServer-0.0.1-SNAPSHOT.jar /app/app.jar

# [수정] Config Server 포트 (예: 8888)
EXPOSE 8888

ENTRYPOINT ["java","-jar","/app/app.jar"]