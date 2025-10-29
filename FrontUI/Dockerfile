FROM eclipse-temurin:17-jre
ENV TZ=Asia/Seoul
WORKDIR /app

# 빌드 스크립트에서 전달하는 JAR_FILENAME 인자를 받도록 선언
ARG JAR_FILENAME

# 선언한 인자를 사용하여 파일을 복사
COPY build/libs/${JAR_FILENAME} /app/app.jar

# 각 서비스에 맞는 포트 노출 (예: FrontUI는 8080)
EXPOSE 14000

ENTRYPOINT ["java","-jar","/app/app.jar"]