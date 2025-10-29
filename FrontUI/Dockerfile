# 실행에 필요한 가벼운 JRE 이미지를 기반으로 합니다.
FROM eclipse-temurin:17-jre

# (필수) 타임존 설정
ENV TZ=Asia/Seoul

# 작업 디렉토리 설정
WORKDIR /app


ARG JAR_FILENAME=unknown.jar

COPY build/libs/${JAR_FILENAME} /app/app.jar


EXPOSE 14000

# 애플리케이션 실행
ENTRYPOINT ["java","-jar","/app/app.jar"]

