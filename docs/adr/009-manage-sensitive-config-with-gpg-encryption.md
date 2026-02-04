# ADR 009: .env 파일 GPG 암호화를 통한 민감한 설정 관리

## 상태

승인됨 (Accepted)

## 컨텍스트

### 문제 상황

- `application-secret.yml`과 `application-prod.yml` 파일이 `.gitignore`에 포함되어 있어 GitHub Actions CI/PR 워크플로우에서 빌드 실패
- Spring Boot가 `application.yml`에서 `include: secret`을 요구하므로, 해당 파일이 없으면 의존성 주입 실패
- **Docker Hub Public Repository 사용**: Private repository 비용 부담으로 public repository 사용 필요
- **Docker 이미지 보안 우려**: JAR 파일에 민감 정보가 포함되면 누구나 이미지를 pull하여 추출 가능

### 팀 상황 및 요구사항

- **팀 구성**: 2명의 개발자
- **설정 파일 동기화**: 팀 내부 페이지에서 기밀 정보를 공유하며 설정 파일 sync를 매우 중요시함
- **핵심 페인포인트**:
  - 배포 환경에서 매번 환경이 바뀔 때마다 PR을 올리기 전에 **환경 설정 문제를 조기에 탐지**하고 싶음
  - 불필요한 부분에 **커밋 로그를 더럽히지 않고** 시간을 절약하고 싶음
  - **Docker Hub Public Repository 사용 시 보안 유지**

---

## 고려한 대안

### 전략 1: 환경별 Profile 분리

- CI 환경에서 별도의 `ci` 프로파일 사용
- **장점**: 간단하고 유지보수가 쉬움
- **단점**: 설정 파일의 구조는 동일하나 값만 다른 프로파일을 추가로 관리해야 하므로, 다른 프로파일의 구조가 변경될 때마다 CI 프로파일도 매번 동기화해야 하는 유지보수 부담 발생

### 전략 2: 템플릿 파일 + GitHub Secrets

- 템플릿 파일 제공 후 CI에서 GitHub Secrets로 실제 파일 생성
- **장점**: CI 환경에서 실제 프로덕션과 유사한 설정으로 검증 가능
- **단점**:
  - Secrets는 텍스트 기반이므로 복잡한 YAML 구조 관리가 번거로움
  - **프로파일이 추가될 때마다 관리 지점이 2배씩 증가** (파일 + Secret)
  - Secret 내용 확인 불가 (한 번 저장하면 볼 수 없음)

### 전략 3: application.yml 파일 GPG 암호화 (초기 채택 → 개선됨)

- `application-secret.yml`, `application-prod.yml`을 GPG로 암호화하여 저장소에 커밋
- CI 워크플로우에서 복호화하여 사용
- **장점**:
  - 실제 파일을 그대로 사용하므로 설정 관리가 간편
  - CI 환경에서 실제 프로덕션과 동일한 설정으로 검증 가능
  - 관리 지점이 1개: GPG_PASSPHRASE만 관리
- **단점**:
  - 파일 수정 시 재암호화 필요
  - **빌드 시 복호화 → JAR에 포함 → Docker 이미지에 포함** ❌
  - **Public Docker Hub 사용 시 보안 위험**: 누구나 이미지를 pull하여 JAR 추출 가능

### 전략 4: .env 파일 GPG 암호화 + spring-dotenv (최종 채택)

- **모든 민감 정보를 `.env` 파일에 통합**하고 GPG로 암호화
- `application.yml`은 환경 변수만 참조 (`${ENV_VAR}` 형식)
- 빌드 시에는 복호화하지 않음 (JAR에 포함 안 됨)
- 배포 시 VM에서 `.env` 파일 복호화하여 사용
- 로컬 개발 시 `spring-dotenv` 라이브러리로 `.env` 파일 자동 로드

**장점**:

- ✅ **Docker 이미지에 민감 정보 절대 미포함**: JAR 파일에 환경 변수 참조만 있음
- ✅ **Public Docker Hub 안전하게 사용 가능**: 이미지를 pull해도 민감 정보 없음
- ✅ **암호화 파일 최소화**: `.env` 파일 하나만 암호화
- ✅ **설정 파일 구조 단순화**: `application-secret.yml` 제거
- ✅ **로컬 개발 편의성**: `spring-dotenv`로 컨테이너 없이 개발 가능
- ✅ **관리 지점 최소화**: GPG_PASSPHRASE 하나만 관리
- ✅ **환경 설정 문제 조기 탐지**: CI에서 실제 환경 변수로 테스트

**단점**:

- 초기 GPG 암호화 설정 필요
- `.env` 파일 수정 시 재암호화 필요 (자동화 스크립트로 해결)
  - 빈도는 낮을 것으로 예상

---

## 결정

**.env 파일 GPG 대칭키 암호화(AES256)를 사용하여 민감한 설정을 관리한다.**

### 핵심 원칙

1. **모든 민감 정보는 `.env` 파일에 통합**
   - `JWT_SECRET_KEY`, `DB_PASSWORD` 등 모든 기밀 정보
   - 환경별 설정 (CORS 등)도 환경 변수로 관리

2. **application.yml은 환경 변수만 참조**
   - 하드코딩된 민감 정보 없음
   - `${ENV_VAR:default}` 형식으로 환경 변수 참조

3. **빌드 시 복호화 안 함**
   - JAR 파일에 민감 정보 미포함
   - Docker 이미지 안전

4. **런타임에 환경 변수 주입**
   - 로컬: `spring-dotenv`로 `.env` 파일 자동 로드
   - 프로덕션: `docker-compose`가 `.env` 파일 읽어서 컨테이너에 주입

---

## 구현 방식

### 1. 파일 구조

#### ❌ 제거된 파일

- `application-secret.yml` (기밀 정보는 `.env`로 이동)
- `application-secret.yml.gpg`
- `application-prod.yml.gpg` (환경별 설정만 포함, 암호화 불필요)

#### ✅ 유지/추가된 파일

- `application.yml` (기본 설정, 환경 변수 참조)
- `application-dev.yml` (개발 환경 전용 - H2 DB 등)
- `application-prod.yml` (프로덕션 환경 전용 - PostgreSQL 등, 환경 변수 참조)
- `.env` (모든 민감 정보, gitignore)
- `.env.gpg` (암호화된 .env, 저장소에 커밋)

### 2. .env 파일 관리

#### .env 파일 내용

```env
SPRING_PROFILES_ACTIVE=prod
DB_USERNAME=blaybus212
DB_PASSWORD=ZZangBeTeam!@12
POSTGRES_DB=blaybus
DOCKER_USERNAME=joonamin44
JWT_SECRET_KEY=gwiV7/hdOd5/RRkpBhlOCQvEp5dejQWf0oFkGOzGg88=
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://frontend-domain.com
```

#### 암호화

```bash
gpg --symmetric --cipher-algo AES256 .env
# → .env.gpg 생성
```

### 3. application.yml 환경 변수 참조

```yaml
# application.yml
spring:
  application:
    name: backend
  profiles:
    active: dev
    # include: secret 제거 (더 이상 필요 없음)

# application-prod.yml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

### 4. 로컬 개발 환경

#### spring-dotenv 추가

```gradle
developmentOnly 'me.paulschwarz:spring-dotenv:4.0.0'
```

- 애플리케이션 시작 시 `.env` 파일 자동 로드
- 개발 환경 전용 (프로덕션 빌드에 미포함)
- 컨테이너 없이 로컬 개발 가능

#### 사용 방법

```bash
# 1. .env 파일 복호화
GPG_PASSPHRASE='passphrase' ./scripts/decrypt-configs.sh

# 2. 애플리케이션 실행
./gradlew bootRun
# spring-dotenv가 자동으로 .env 로드
```

### 5. CI/CD 통합

#### CI (ci-pr.yml)

- `.env` 파일 복호화하지 않음
- 테스트용 환경 변수 직접 설정
- JAR 빌드 시 민감 정보 미포함

```yaml
- name: Test with Gradle
  run: ./gradlew test --no-daemon
  env:
    JWT_SECRET_KEY: test-secret-key-for-ci-only
```

#### 배포 (deploy.yml)

- `.env.gpg` 파일을 VM에 복사
- VM에서 복호화하여 사용
- `docker-compose`가 `.env` 파일 읽어서 컨테이너에 주입

```yaml
# 1. .env.gpg 복사
source: "docker-compose.yml,prometheus.yml,grafana/,.env.gpg"

# 2. VM에서 복호화
gpg --decrypt --output .env .env.gpg
chmod 600 .env

# 3. docker-compose 실행 (.env 자동 인식)
docker compose up -d
```

### 6. docker-compose.yml 환경 변수 주입

```yaml
services:
  web-server:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/${POSTGRES_DB}
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
```

### 7. .gitignore 설정

```gitignore
# local environment variables
**/.env
!.env.gpg  # 암호화된 .env 파일은 커밋 허용

# application-prod.yml은 더 이상 gitignore하지 않음
# (환경별 설정만 포함, 기밀 정보 없음)
```

---

## 결과

### 긍정적 영향

#### ✅ Docker 이미지 보안

- **JAR 파일에 민감 정보 절대 미포함**
- Public Docker Hub 안전하게 사용 가능
- 누구나 이미지를 pull해도 안전

#### ✅ 환경 설정 문제 조기 탐지

- CI에서 실제 환경 변수로 테스트
- 배포 전 설정 문제 발견 가능

#### ✅ 관리 지점 최소화

- 암호화 파일: `.env.gpg` 하나만
- GitHub Secrets: `GPG_PASSPHRASE` 하나만
- 프로파일 추가 시 `.env`에 변수만 추가

#### ✅ 커밋 로그 정리

- 설정 변경 시 `.env.gpg` 하나만 커밋
- 불필요한 커밋 최소화

#### ✅ 로컬 개발 편의성

- `spring-dotenv`로 컨테이너 없이 개발 가능
- `.env` 파일만 복호화하면 즉시 실행

#### ✅ 설정 파일 구조 단순화

- `application-secret.yml` 제거
- 환경 변수 기반 통합 관리

### 부정적 영향

- ⚠️ GPG 설치 필요 (대부분의 개발 환경에 기본 설치됨)
- ⚠️ `.env` 파일 수정 시 재암호화 필요 (자동화 스크립트로 완화)
- ⚠️ Passphrase 관리 필요 (팀 내부 비밀번호 관리 도구 활용)

### 트레이드오프

#### 복잡도 vs 보안

- **초기 설정 복잡도 증가**: GPG 암호화, spring-dotenv 설정
- **보안 대폭 향상**: Docker 이미지에 민감 정보 미포함, Public Docker Hub 안전 사용

#### 재암호화 필요 vs 관리 지점 최소화

- **파일 수정 시 재암호화 필요**: `.env` 수정 후 재암호화
- **관리 지점 최소화**: `.env` 파일 하나만 암호화, GitHub Secrets 방식보다 훨씬 적음

#### 환경 변수 기반 vs 파일 기반

- **환경 변수 기반**: 런타임에 주입, 유연성 높음
- **파일 기반 암호화**: 버전 관리 용이, 팀 협업 편리

---

## 의사결정 진화 과정

### Phase 1: application.yml 암호화 (초기)

```
application-secret.yml.gpg → 빌드 시 복호화 → JAR에 포함 → Docker 이미지에 포함 ❌
```

- **문제**: Docker 이미지에 민감 정보 포함
- **위험**: Public Docker Hub 사용 불가

### Phase 2: .env 파일 암호화 (개선)

```
.env.gpg → 빌드 시 복호화 안 함 → JAR에 미포함 → Docker 이미지 안전 ✅
런타임에 VM에서 복호화 → 환경 변수로 주입 ✅
```

- **개선**: Docker 이미지 보안 확보
- **효과**: Public Docker Hub 안전 사용

### Phase 3: spring-dotenv 추가 (최종)

```
로컬: spring-dotenv로 .env 자동 로드 ✅
프로덕션: docker-compose가 .env 읽어서 주입 ✅
```

- **개선**: 로컬 개발 편의성 향상
- **효과**: 컨테이너 없이 개발 가능

---

## 보안 고려사항

### Docker 이미지 보안

- ✅ JAR 파일에 민감 정보 미포함
- ✅ `application.yml`은 환경 변수 참조만 (`${JWT_SECRET_KEY}`)
- ✅ Public Docker Hub 안전 사용 가능

### 런타임 보안

- ✅ `.env` 파일은 VM에서만 복호화
- ✅ 파일 권한 600으로 설정 (소유자만 읽기/쓰기)
- ✅ 컨테이너는 환경 변수로만 접근

### Git 저장소 보안

- ✅ `.env.gpg`만 커밋
- ✅ 원본 `.env`는 gitignore
- ✅ `application-prod.yml`은 기밀 정보 없으므로 커밋 가능

---

## 참고 자료

- 자동화 스크립트: `scripts/encrypt-configs.sh`, `scripts/decrypt-configs.sh`
- Docker 보안 분석: `docs/security/docker-credential-security-analysis.md`
- 개발자 가이드: `docs/setup-guide.md`
