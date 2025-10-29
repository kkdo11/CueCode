# 실행에 필요한 가벼운 JRE 이미지를 기반으로 합니다.
FROM eclipse-temurin:17-jre

# 타임존 설정
ENV TZ=Asia/Seoul

# 작업 디렉토리 설정
WORKDIR /app

# [수정 확인!] FrontUI .jar 파일 이름을 직접 명시합니다.
# build.gradle 버전 변경 시 여기도 수정해야 합니다.
COPY build/libs/FrontUI-0.0.1-SNAPSHOT.jar /app/app.jar

# [수정 확인!] FrontUI 실제 포트 번호로 변경하세요. (예: 8000)
EXPOSE 14000

# 애플리케이션 실행
ENTRYPOINT ["java","-jar","/app/app.jar"]
