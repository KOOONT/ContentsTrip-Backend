FROM openjdk:17-jdk
# 3. JAR 파일 경로를 ARG로 지정하여 유연하게 설정
ARG JAR_FILE=build/libs/HeritageLoad-0.0.1-SNAPSHOT.jar

# 4. JAR 파일을 도커 이미지에 복사
COPY ${JAR_FILE} heritageload-springboot.jar

# 5. ENTRYPOINT에서 urandom을 사용하여 빠른 난수 생성 및 실행
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/heritageload-springboot.jar"]

# 6. 애플리케이션이 사용하는 포트 노출
EXPOSE 8080