# 실행에 필요한 가벼운 JRE 이미지를 기반으로 합니다.
FROM eclipse-temurin:17-jre

# (필수) 타임존 설정
ENV TZ=Asia/Seoul

# 작업 디렉토리 설정
WORKDIR /app

# [수정] 빌드 스크립트에서 '--build-arg'로 전달해주는 .jar 파일 경로를 받습니다.
ARG JAR_FILE=build/libs/unknown.jar
COPY ${JAR_FILE} /app/app.jar

# [필수] 이 포트 번호는 각 서비스의 Dockerfile마다 실제 서비스 포트로 수정해야 합니다.
# (예: ApiGateway는 8080, UserService는 8001, ConfigServer는 8888 등)
EXPOSE 14000

# 애플리케이션 실행
ENTRYPOINT ["java","-jar","/app/app.jar"]
