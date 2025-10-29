# 실행에 필요한 가벼운 JRE 이미지를 기반으로 합니다.
FROM eclipse-temurin:17-jre

# (필수) 타임존 설정
ENV TZ=Asia/Seoul

# 작업 디렉토리 설정
WORKDIR /app

# [수정 확인!] 빌드 스크립트에서 '--build-arg'로 전달해주는 .jar '파일명'을 받습니다.
ARG JAR_FILENAME=unknown.jar
# [수정 확인!] COPY 명령어에서는 디렉토리 경로를 명시하고, 파일명 변수를 사용합니다.
COPY build/libs/${JAR_FILENAME} /app/app.jar

# [필수] Config Server 포트 번호로 수정 (예: 8888)
EXPOSE 13000

# 애플리케이션 실행
ENTRYPOINT ["java","-jar","/app/app.jar"]