# 1. 빌드 스테이지: Gradle을 사용하여 JAR 파일 생성
FROM gradle:8-jdk17 AS build
WORKDIR /app

# 캐시 효율을 위해 빌드 파일들을 먼저 복사
COPY build.gradle.kts settings.gradle.kts gradlew /app/
COPY gradle /app/gradle

# 의존성 먼저 다운로드 (선택 사항이지만 권장)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 JAR 빌드
COPY src /app/src
RUN ./gradlew bootJar --no-daemon

# 2. 실행 스테이지: 가벼운 JRE 이미지를 사용하여 실행
FROM openjdk:17-jdk-slim
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=build /app/build/libs/HeritageLoad-0.0.1-SNAPSHOT.jar app.jar

# ENTRYPOINT에서 urandom을 사용하여 빠른 난수 생성 및 실행
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

# 애플리케이션이 사용하는 포트 노출
EXPOSE 8080