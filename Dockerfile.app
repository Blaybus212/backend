# web application 전용 dockerfile (Ubuntu 22.04 기반)
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# 필수 도구 및 Node.js 22 설치 (Nodesource 사용)
RUN apt-get update && apt-get install -y curl gnupg \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs wget unzip \
    && rm -rf /var/lib/apt/lists/*

# Gradle 설치 (Ubuntu 환경에서의 명시적 버전 관리)
ENV GRADLE_VERSION=9.3.0
RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
    && unzip gradle-${GRADLE_VERSION}-bin.zip \
    && mv gradle-${GRADLE_VERSION} /opt/gradle \
    && ln -s /opt/gradle/bin/gradle /usr/bin/gradle

# 빌드 테스트를 위한 Node.js 의존성 사전 설치
COPY src/main/resources/scripts/package.json ./
RUN npm install
ENV NODE_PATH=/app/node_modules

COPY build.gradle settings.gradle ./
# 의존성만 미리 다운로드하여 레이어 캐시 활용
RUN gradle dependencies --no-daemon

COPY src ./src
# 테스트를 통과하면 실행 가능한 jar 파일만 빌드
RUN gradle test bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Node.js 22 및 환경 구축 (Ubuntu 기반)
RUN apt-get update && apt-get install -y curl gnupg \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# 애플리케이션 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# GLTF 조립에 필요한 Node.js 라이브러리 설치
COPY src/main/resources/scripts/package.json ./
RUN npm install && rm package.json

# 환경 변수 설정
ENV NODE_PATH=/app/node_modules
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
