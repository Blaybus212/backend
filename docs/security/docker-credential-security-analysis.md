# Docker 이미지 내 Credential 유출 가능성 분석

## 현재 상황 분석

### Dockerfile.app 구조

```dockerfile
# 빌드 단계
FROM gradle:9.3.0-jdk21 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon
COPY src ./src
RUN gradle test bootJar --no-daemon

# 실행 단계
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## ✅ 안전한 이유

### 1. 멀티 스테이지 빌드 사용

- **빌더 단계**: `src/` 디렉토리를 복사하여 빌드
- **최종 이미지**: **JAR 파일만** 복사됨
- **결과**: 원본 소스 코드(`src/`)는 최종 이미지에 포함되지 않음

### 2. Spring Boot JAR 파일의 특성

Spring Boot는 설정 파일을 JAR 내부에 패키징합니다:

- `application-secret.yml` → JAR 내부 `BOOT-INF/classes/application-secret.yml`
- `application-prod.yml` → JAR 내부 `BOOT-INF/classes/application-prod.yml`

**중요**: JAR 파일은 ZIP 형식이므로 압축 해제하면 내용을 볼 수 있습니다!

## ⚠️ 잠재적 위험

### 위험 1: Docker 이미지 공개 저장소 업로드

현재 Docker Hub에 이미지를 푸시하고 있습니다:

```yaml
tags: |
  ${{ secrets.DOCKER_USERNAME }}/blaybus-backend:latest
```

**만약 이 저장소가 public이라면:**

1. 누구나 이미지를 pull 가능
2. JAR 파일 추출: `docker cp <container>:/app/app.jar .`
3. JAR 압축 해제: `unzip app.jar`
4. 설정 파일 확인: `cat BOOT-INF/classes/application-secret.yml`

### 위험 2: Docker 이미지 레이어 분석

Docker 이미지는 레이어 구조로 되어 있어, 각 레이어를 분석할 수 있습니다:

```bash
docker history <image>
docker save <image> -o image.tar
tar -xf image.tar
```

## 🛡️ 보안 대책

### 현재 적용된 대책

#### 1. GPG 암호화 파일만 저장소에 커밋

- ✅ 원본 `.yml` 파일은 gitignore
- ✅ `.gpg` 파일만 저장소에 존재
- ✅ 빌드 시에만 복호화하여 JAR에 포함

#### 2. 멀티 스테이지 빌드

- ✅ 소스 코드는 최종 이미지에 포함되지 않음
- ✅ JAR 파일만 최종 이미지에 포함

### 추가 권장 대책

#### 1. Docker Hub Private Repository 사용 (강력 권장)

```yaml
# Docker Hub에서 repository를 private으로 설정
# Settings → Visibility → Private
```

**이유**: Public repository는 누구나 이미지를 pull하여 JAR 파일을 추출할 수 있음

#### 2. 환경 변수 기반 설정 (선택사항)

민감한 정보를 JAR에 포함하지 않고 런타임에 주입:

**application-secret.yml**:

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY} # 환경 변수에서 주입
```

**docker-compose.yml**:

```yaml
services:
  backend:
    environment:
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
```

**장점**: JAR 파일에 민감 정보가 포함되지 않음
**단점**: 환경 변수 관리 복잡도 증가

#### 3. Docker Content Trust 활성화 (선택사항)

이미지 서명 및 검증:

```bash
export DOCKER_CONTENT_TRUST=1
docker push <image>
```

## 🔍 현재 구현의 보안 수준

### ✅ 안전한 부분

1. **Git 저장소**: 암호화된 파일만 커밋되므로 안전
2. **빌드 프로세스**: CI에서만 복호화하므로 안전
3. **멀티 스테이지 빌드**: 소스 코드 노출 없음

### ⚠️ 주의 필요한 부분

1. **Docker 이미지**: JAR 파일에 설정 파일이 포함됨
   - **해결책**: Docker Hub repository를 **private**으로 설정

### 결론

**현재 구현은 다음 조건에서 안전합니다:**

- ✅ Docker Hub repository가 **private**인 경우
- ✅ 이미지 접근 권한이 팀원으로 제한된 경우

**만약 Docker Hub repository가 public이라면:**

- ❌ 누구나 이미지를 pull하여 JAR 파일 추출 가능
- ❌ JAR 내부의 `application-secret.yml` 확인 가능
- 🛡️ **즉시 private으로 변경 필요**

## 권장 조치

1. **즉시**: Docker Hub repository를 private으로 설정
2. **선택사항**: 매우 민감한 정보는 환경 변수로 런타임 주입 고려
3. **모니터링**: Docker Hub 접근 로그 주기적 확인
