# web application 전용 dockerfile
FROM gradle:9.3.0-jdk21 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
# 테스트를 통과하면 실행 가능한 jar 파일만 빌드
RUN gradle test bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
