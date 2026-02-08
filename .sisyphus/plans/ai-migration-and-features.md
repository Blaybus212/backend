# AI 코드 마이그레이션 + 미구현 기능 통합 작업 계획

## TL;DR

> **Quick Summary**: feat/23-add-entity 머지로 변경된 엔티티 구조에 기존 AI 코드를 마이그레이션한 후, 나머지 3개 AI 기능(User 개인화 프롬프트, 퀴즈 임베딩 채점, 토큰 사용량 모니터링)을 구현한다.
> 
> **Deliverables**:
> - AI 코드가 새 엔티티 패키지(`domain/conversation/`, `domain/alignment/`)를 올바르게 참조
> - PromptService에 persona 4종 시스템 프롬프트 + education_level/specialized_in 반영
> - `POST /scenes/{sceneId}/quiz/{quizId}/grade` 엔드포인트 (SELECT=정답비교, INPUT=임베딩 채점)
> - OpenAI 토큰 사용량 Micrometer 카운터 → `/actuator/prometheus` 노출
> 
> **Estimated Effort**: Large
> **Parallel Execution**: NO — 순차 실행 (마이그레이션 → #7 → #8 → #9)
> **Critical Path**: Task 0 → Task 1 → Task 2 → Task 3

---

## Context

### Original Request
feat/23-add-entity 브랜치를 develop에 머지한 후, 엔티티 구조 변경에 맞춰 기존 AI 코드를 마이그레이션하고, 미구현 태스크 #7(User 개인화 프롬프트), #8(주관식 퀴즈 Embedding 채점), #9(토큰 사용량 모니터링)를 구현한다.

### Interview Summary
**Key Discussions**:
- 머지 후 OLD 엔티티(domain/)와 NEW 엔티티(domain/conversation/ 등)가 공존 → OLD 삭제 + AI 코드 import 전환 필요
- Conversation.summary: `nullable = false` → `nullable = true`로 변경, `updateSummary()` 메서드 추가
- 퀴즈 채점: SELECT=정답 비교, INPUT=OpenAI Embedding cosine similarity (threshold >= 0.8)
- 퀴즈 완료 후 재채점 허용
- 테스트 없이 진행 — 컴파일 + curl 검증만

**Research Findings**:
- Quiz 엔티티(SceneRanksQuiz, SceneRanksQuizChoice, QuizUserProgress, QuizType) 이미 존재
- `embedding-model: text-embedding-3-small` application.yml에 이미 설정됨
- Micrometer + Prometheus 의존성 이미 포함
- User.persona/educationLevel/specializedIn 필드 이미 존재
- OpenAiConfig의 ObjectMapper 빈이 Spring Boot 기본 설정을 오버라이드 — 제거 필요

### Metis Review
**Identified Gaps** (addressed):
- Conversation.summary nullable 문제 → 사용자 결정: nullable로 변경
- Conversation.summary 업데이트 메커니즘 → 사용자 결정: updateSummary() 메서드 추가
- Message @PrePersist 제거 → postedAt 명시적 설정으로 해결
- ConversationRepository 쿼리 변경 → @Query with scene.id JPQL
- QuizType @Enumerated 누락 → 추가
- ObjectMapper 빈 중복 → 제거

---

## Work Objectives

### Core Objective
AI 코드를 새 엔티티 구조에 마이그레이션하고, 3개 미구현 AI 기능(개인화, 퀴즈 채점, 토큰 모니터링)을 완성한다.

### Concrete Deliverables
- OLD 엔티티 파일 4개 삭제, AI 코드 import 전환 완료
- PromptService에 persona 4종 시스템 프롬프트 주입
- QuizController + QuizGradingService + QuizRepository + 관련 DTO
- OpenAiService에 토큰 사용량 Micrometer 카운터

### Definition of Done
- [ ] `./gradlew spotlessApply compileJava --no-daemon` 성공
- [ ] `./gradlew checkstyleMain --no-daemon` 성공
- [ ] OLD 엔티티 파일 0개 (domain/ 루트에 엔티티 없음)
- [ ] curl로 대화 엔드포인트 정상 동작 확인
- [ ] curl로 퀴즈 채점 엔드포인트 정상 동작 확인
- [ ] `/actuator/prometheus`에 openai 토큰 카운터 노출 확인

### Must Have
- 새 엔티티 패키지(`domain/conversation/*`, `domain/alignment/*`, `domain/quiz/*`) 기준으로 모든 import 통일
- persona별 시스템 프롬프트 4종 (senior/friend/professor/assistant)
- persona/educationLevel/specializedIn null 시 기본 프롬프트 fallback
- SELECT 퀴즈: answer 필드와 정확히 비교
- INPUT 퀴즈: Embedding cosine similarity >= 0.8 정답
- QuizUserProgress 업데이트 (재채점 허용)
- Micrometer Counter로 input_tokens / output_tokens 집계

### Must NOT Have (Guardrails)
- ❌ `@Table` 또는 `@Column` name 변경 금지 (스키마 고정)
- ❌ 새 `@Entity` 클래스 생성 금지 (모든 엔티티 이미 존재)
- ❌ Message에 `@OneToMany references` 다시 추가 금지 (NEW 설계 존중)
- ❌ SceneInformationRepository 생성 금지 (JPQL로 scene.id 조회)
- ❌ RAG/벡터 DB/벡터 검색 금지
- ❌ 퀴즈 CRUD(생성/수정/삭제) 금지 — 채점만
- ❌ 토큰 사용량 DB 저장/대시보드/rate limiting 금지
- ❌ `OpenAI` 대문자 명명 금지 → `OpenAi` 사용
- ❌ `@AuthenticationPrincipal String` 사용 금지 → `CustomUserDetails`
- ❌ Chat Completions API 사용 금지 → Responses API (`/v1/responses`)
- ❌ `spring-boot-starter-json` 사용 금지 → `jackson-databind`
- ❌ `checkstyleTest` 실행 금지
- ❌ ChatController 개인화 적용 금지 (별도 테스트 엔드포인트로 유지)
- ❌ ObjectMapper 빈 유지 금지 → OpenAiConfig에서 제거

---

## Verification Strategy (MANDATORY)

> **UNIVERSAL RULE: ZERO HUMAN INTERVENTION**
>
> ALL tasks in this plan MUST be verifiable WITHOUT any human action.

### Test Decision
- **Infrastructure exists**: NO
- **Automated tests**: None
- **Framework**: none

### Agent-Executed QA Scenarios (MANDATORY — ALL tasks)

> 테스트 없이 진행하므로 Agent-Executed QA가 PRIMARY 검증 방법.
> 모든 태스크에 컴파일 게이트 + curl 통합 검증 포함.

**Verification Tool by Deliverable Type:**

| Type | Tool | How Agent Verifies |
|------|------|-------------------|
| **Java 코드 변경** | Bash (`./gradlew compileJava`) | 컴파일 성공 여부 |
| **API 엔드포인트** | Bash (curl) | HTTP 상태 코드 + 응답 JSON 구조 |
| **Prometheus 메트릭** | Bash (curl /actuator/prometheus) | 메트릭 키 존재 여부 |
| **파일 삭제/존재** | Bash (ls/find) | 파일 존재/부재 확인 |

---

## Branch Strategy

> **작업 브랜치**: `ai-feat` (develop에서 생성)
> 
> **FIRST ACTION (모든 Task 시작 전)**:
> 1. `git checkout -b ai-feat` — 새 브랜치 생성 (현재 develop 워킹 디렉토리 변경사항 자동 포함)
> 2. 이후 모든 커밋은 `ai-feat` 브랜치에서 진행
> 
> **커밋 규칙**:
> - 커밋 메시지는 **한 문장, 한국어**로 작성
> - prefix는 기존 규칙 유지 (feat, refactor, fix 등)
> - 예: `refactor(ai): 새 엔티티 패키지 구조에 맞춰 AI 코드 마이그레이션`

---

## Execution Strategy

### Sequential Execution (No Parallel)

```
[Branch: ai-feat 생성 from develop]
  ↓
Task 0: AI 코드 마이그레이션 (블로킹 — 이후 모든 작업의 전제)
  ↓
Task 1: User 개인화 프롬프트 (#7)
  ↓
Task 2: 퀴즈 임베딩 채점 (#8)
  ↓
Task 3: 토큰 사용량 모니터링 (#9)
```

### Dependency Matrix

| Task | Depends On | Blocks | Can Parallelize With |
|------|------------|--------|---------------------|
| 0 | None | 1, 2, 3 | None |
| 1 | 0 | None | None (PromptService 변경이 2, 3에 영향 없지만 안전하게 순차) |
| 2 | 0 | None | 3 (가능하지만 OpenAiService 동시 수정 위험) |
| 3 | 0 | None | 2 (가능하지만 OpenAiService 동시 수정 위험) |

### Agent Dispatch Summary

| Task | Recommended Agent |
|------|-------------------|
| 0 | `category="quick"` — 기계적 import 변경, 파일 삭제 |
| 1 | `category="unspecified-low"` — PromptService 수정, 프롬프트 하드코딩 |
| 2 | `category="unspecified-high"` — 새 서비스/컨트롤러/DTO/리포지토리 다수 생성 |
| 3 | `category="quick"` — DTO 필드 추가 + Micrometer 카운터 |

---

## TODOs

- [ ] 0. AI 코드 엔티티 마이그레이션 + 커밋

  **What to do**:

  **0-A. OLD 엔티티 파일 삭제** (반드시 FIRST ACTION):
  - `src/main/java/com/blaybus/backend/domain/Conversation.java` 삭제
  - `src/main/java/com/blaybus/backend/domain/Message.java` 삭제
  - `src/main/java/com/blaybus/backend/domain/Reference.java` 삭제
  - `src/main/java/com/blaybus/backend/domain/Component.java` 삭제

  **0-B. NEW Conversation 엔티티 수정** (`domain/conversation/Conversation.java`):
  - `@Column(name = "summary", nullable = false)` → `@Column(name = "summary", nullable = true)` 로 변경
  - `public void updateSummary(String summary) { this.summary = summary; }` 메서드 추가

  **0-C. SceneRanksQuiz 엔티티 수정** (`domain/quiz/SceneRanksQuiz.java`):
  - `type` 필드에 `@Enumerated(EnumType.STRING)` 추가 (누락된 어노테이션)

  **0-D. AI 코드 import 전환** — 아래 파일들의 import 경로 변경:

  | 파일 | OLD import | NEW import |
  |------|-----------|------------|
  | ConversationService.java | `domain.Conversation` | `domain.conversation.Conversation` |
  | ConversationService.java | `domain.Message` | `domain.conversation.Message` |
  | ConversationService.java | `domain.Reference` | `domain.conversation.Reference` |
  | ConversationService.java | `domain.Component` | `domain.alignment.Component` |
  | ConversationService.java | (inner) `Message.Sender` | `domain.conversation.Sender` |
  | PromptService.java | `domain.Component` | `domain.alignment.Component` |
  | ConversationDto.java | `domain.Component` | `domain.alignment.Component` |
  | ConversationDto.java | `domain.Message` | `domain.conversation.Message` |
  | ConversationRepository.java | `domain.Conversation` | `domain.conversation.Conversation` |
  | ConversationRepository.java | `domain.user.User` | (이미 올바름 — 확인만) |
  | MessageRepository.java | `domain.Conversation` | `domain.conversation.Conversation` |
  | MessageRepository.java | `domain.Message` | `domain.conversation.Message` |
  | ReferenceRepository.java | `domain.Reference` | `domain.conversation.Reference` |
  | ComponentRepository.java | `domain.Component` | `domain.alignment.Component` |

  **0-E. ConversationService 비즈니스 로직 수정**:
  - `conversation.getRunningSummary()` → `conversation.getSummary()` 변경
  - `conversation.updateRunningSummary(...)` → `conversation.updateSummary(...)` 변경
  - `Conversation.builder().sceneId(sceneId)` → SceneInformation 조회 없이 `@Query`로 처리하도록 변경
    - ConversationRepository에 `@Query("SELECT c FROM Conversation c WHERE c.user = :user AND c.scene.id = :sceneId")` 추가
    - Conversation 생성 시: SceneInformation을 참조해야 하므로, SceneInformation을 JPA `entityManager.getReference(SceneInformation.class, sceneId)` 또는 `sceneInformationRepository.getReferenceById(sceneId)` 로 프록시 로드 (DB 조회 없이 FK만 설정)
    - **주의**: SceneInformationRepository가 없으므로 생성 필요 — 단, 이는 `JpaRepository<SceneInformation, Long>` 인터페이스만 생성하고 `getReferenceById()` 만 사용 (커스텀 메서드 없음)
  - `Message.Sender.USER` → `Sender.USER` 변경 (외부 enum import)
  - `Message.Sender.ASSISTANT` → `Sender.ASSISTANT` 변경
  - Message 생성 시 `.postedAt(LocalDateTime.now())` 명시적 설정 추가
  - Reference 생성 시 `message.addReference(ref)` 호출 제거 → `referenceRepository.save(ref)` 로 직접 저장
  - ConversationDto의 MessageResponse 매핑에서 `message.getSender().name()` → Sender enum 직접 참조

  **0-F. OpenAiConfig에서 ObjectMapper 빈 제거**:
  - `@Bean public ObjectMapper objectMapper() { ... }` 삭제 (Spring Boot 기본 설정 사용)
  - OpenAiService에서 ObjectMapper를 직접 주입받는 부분이 있다면 그대로 유지 (Spring Boot 기본 빈 사용)

  **0-G. 모든 AI 관련 수정/신규 파일을 git add + commit**:
  - build.gradle, application.yml, SecurityConfig, CommonErrorCode 변경사항 포함
  - 신규 AI 파일들 (services, controllers, repositories, DTOs, config, docs) 포함
  - SceneInformationRepository (새로 생성) 포함
  - OLD 엔티티 삭제 포함

  **Must NOT do**:
  - Message에 `@OneToMany references` 다시 추가하지 않는다
  - ConversationService에 기능 추가/리팩토링하지 않는다 — import 변경과 필드 접근만
  - Conversation, Message, Reference 엔티티의 `@Table`, `@Column` name 변경하지 않는다 (summary nullable 변경 + updateSummary 추가만 허용)
  - `addReference()` 메서드를 Message에 다시 추가하지 않는다

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 기계적 import 변경 + 파일 삭제 + 소규모 비즈니스 로직 수정. 판단이 필요한 새 설계가 아님.
  - **Skills**: [`git-master`]
    - `git-master`: 커밋 관리에 필요

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential — Task 0 is the foundation
  - **Blocks**: Tasks 1, 2, 3
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `src/main/java/com/blaybus/backend/domain/conversation/Conversation.java` — NEW 엔티티 구조. `@ManyToOne SceneInformation scene` 필드 확인, `@Column(name = "summary")` 수정 대상
  - `src/main/java/com/blaybus/backend/domain/conversation/Message.java` — NEW Message 엔티티. `@Builder` 패턴, `Sender` 외부 enum 사용 확인
  - `src/main/java/com/blaybus/backend/domain/conversation/Reference.java` — NEW Reference. `@ManyToOne` alignment.Component 참조
  - `src/main/java/com/blaybus/backend/domain/conversation/Sender.java` — 외부 enum (USER, ASSISTANT)
  - `src/main/java/com/blaybus/backend/domain/alignment/Component.java` — NEW Component. `usage` 컬럼 (not `usage_info`)
  - `src/main/java/com/blaybus/backend/domain/Scene/SceneInformation.java` — FK 대상 엔티티. 패키지 주의: 디렉토리는 `Scene/`(대문자), 코드는 `domain.scene`(소문자)

  **수정 대상 파일 (AI 코드)**:
  - `src/main/java/com/blaybus/backend/service/ConversationService.java` — import 전환 + 비즈니스 로직 수정 (가장 큰 변경)
  - `src/main/java/com/blaybus/backend/service/PromptService.java` — Component import만 변경
  - `src/main/java/com/blaybus/backend/dto/ConversationDto.java` — Component, Message import 변경
  - `src/main/java/com/blaybus/backend/repository/ConversationRepository.java` — import + @Query 추가
  - `src/main/java/com/blaybus/backend/repository/MessageRepository.java` — import 변경
  - `src/main/java/com/blaybus/backend/repository/ReferenceRepository.java` — import 변경
  - `src/main/java/com/blaybus/backend/repository/ComponentRepository.java` — import 변경
  - `src/main/java/com/blaybus/backend/config/OpenAiConfig.java` — ObjectMapper 빈 제거

  **Acceptance Criteria**:

  **Agent-Executed QA Scenarios:**

  ```
  Scenario: 컴파일 성공 확인
    Tool: Bash
    Preconditions: JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
    Steps:
      1. ./gradlew spotlessApply compileJava --no-daemon
      2. Assert: stdout contains "BUILD SUCCESSFUL"
    Expected Result: 컴파일 에러 없음
    Evidence: 빌드 출력 캡처

  Scenario: OLD 엔티티 파일 삭제 확인
    Tool: Bash
    Steps:
      1. ls src/main/java/com/blaybus/backend/domain/Conversation.java 2>&1
      2. ls src/main/java/com/blaybus/backend/domain/Message.java 2>&1
      3. ls src/main/java/com/blaybus/backend/domain/Reference.java 2>&1
      4. ls src/main/java/com/blaybus/backend/domain/Component.java 2>&1
      5. Assert: 모든 ls 결과가 "No such file or directory"
    Expected Result: 4개 파일 모두 삭제됨
    Evidence: ls 출력 캡처

  Scenario: OLD import 잔재 없음 확인
    Tool: Bash
    Steps:
      1. grep -r "import com.blaybus.backend.domain.Conversation;" src/main/java/ | wc -l
      2. grep -r "import com.blaybus.backend.domain.Message;" src/main/java/ | wc -l
      3. grep -r "import com.blaybus.backend.domain.Component;" src/main/java/ | wc -l
      4. grep -r "import com.blaybus.backend.domain.Reference;" src/main/java/ | wc -l
      5. Assert: 모두 0
    Expected Result: OLD 패키지 import 0건
    Evidence: grep 결과 캡처

  Scenario: 서버 기동 + 대화 엔드포인트 동작 확인
    Tool: Bash
    Preconditions: OPENAI_API_KEY 환경변수 설정됨
    Steps:
      1. ./gradlew bootRun --no-daemon & (백그라운드)
      2. Wait for: "Started BackendApplication" in output (timeout: 60s)
      3. curl -s -X POST http://localhost:8080/scenes/1/chat \
           -H "Content-Type: application/json" \
           -d '{"message":"테스트"}'
      4. Assert: HTTP 200, response contains "answer" field
      5. Kill server process
    Expected Result: AI 응답 정상 반환
    Evidence: curl 응답 캡처
  ```

  **Commit**: YES
  - Message: `refactor(ai): 새 엔티티 패키지 구조에 맞춰 AI 코드 마이그레이션`
  - Files: 모든 AI 관련 파일 (services, controllers, repositories, DTOs, config, docs, entity 수정, OLD 삭제)
  - Pre-commit: `./gradlew spotlessApply compileJava --no-daemon`

---

- [ ] 1. User 개인화 프롬프트 (#7)

  **What to do**:

  **1-A. PromptService.java 수정**:
  - `buildSystemPrompt(Long sceneId)` 시그니처를 `buildSystemPrompt(Long sceneId, User user)` 로 변경
  - User.persona에 따른 시스템 프롬프트 4종 추가:

  ```
  SENIOR (든든한 선배):
  "너는 SIMVEX에서 일하는 든든한 선배야. 후배가 부품에 대해 질문하면...
  - 공감적, 실무 중심, '나도 처음엔 그랬어' 스타일
  - 반말 사용, 친근하게"

  FRIEND (호기심 많은 친구):
  "너는 SIMVEX에서 같이 일하는 호기심 많은 친구야...
  - 에너지 넘침, '와 대박!', 시각적 탐색 유도
  - 반말, 이모지 적극 사용"

  PROFESSOR (열정적인 교수님):
  "당신은 SIMVEX의 기술 교수입니다...
  - 학문적 깊이, 칭찬, 연결성 강조
  - 격식체 사용"

  ASSISTANT (이성적인 도우미):
  "당신은 SIMVEX의 기술 어시스턴트입니다...
  - 데이터 중심, 감정 배제
  - '정의-구조-유사 사례' 순서
  - 격식체"
  ```

  - User.persona가 null인 경우 기존 SYSTEM_PROMPT_TEMPLATE(기본 프롬프트) 사용
  - User.educationLevel이 null이 아닌 경우 프롬프트에 학습 수준 컨텍스트 추가:
    - BEGINNER: "사용자는 입문자입니다. 쉬운 용어와 비유를 사용해 설명해주세요."
    - FUNDAMENTAL: "사용자는 기초 수준입니다. 기본 개념은 알고 있으니 핵심 위주로 설명해주세요."
    - INTERMEDIATE: "사용자는 중급자입니다. 전문 용어를 사용해도 됩니다."
    - EXPERT: "사용자는 전문가입니다. 심화 내용과 기술적 디테일을 제공해주세요."
  - User.specializedIn이 null이 아니고 비어있지 않으면 프롬프트에 추가:
    - "사용자의 전공/전문 분야: {specializedIn}. 이 배경지식을 고려해 설명해주세요."

  **1-B. ConversationService.java 수정**:
  - `promptService.buildSystemPrompt(sceneId)` → `promptService.buildSystemPrompt(sceneId, user)` 호출 변경
  - `user` 변수는 이미 `sendMessage()` 메서드 스코프에 존재하므로 추가 로딩 불필요

  **Must NOT do**:
  - ChatController는 수정하지 않는다 (별도 테스트 엔드포인트)
  - 프롬프트를 외부 설정(yml)으로 분리하지 않는다 — Java 코드에 하드코딩
  - 프롬프트 캐싱 로직 추가하지 않는다
  - 새 엔티티/테이블 생성하지 않는다

  **Recommended Agent Profile**:
  - **Category**: `unspecified-low`
    - Reason: PromptService 1개 파일 주요 수정 + ConversationService 1줄 변경. 프롬프트 텍스트 작성이 주 작업.
  - **Skills**: [`git-master`]
    - `git-master`: 커밋 관리

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Task 0 완료 후)
  - **Blocks**: None
  - **Blocked By**: Task 0

  **References**:

  **Pattern References**:
  - `src/main/java/com/blaybus/backend/service/PromptService.java` — 현재 시스템 프롬프트 템플릿과 buildSystemPrompt() 메서드 구조 확인
  - `src/main/java/com/blaybus/backend/service/ConversationService.java:125-128` — promptService.buildSystemPrompt() 호출 위치
  - `src/main/java/com/blaybus/backend/domain/user/User.java:49-59` — persona, educationLevel, specializedIn 필드 위치와 타입

  **API/Type References**:
  - `src/main/java/com/blaybus/backend/domain/user/Persona.java` — Persona enum: SENIOR, PROFESSOR, FRIEND, ASSISTANT
  - `src/main/java/com/blaybus/backend/domain/user/EducationLevel.java` — EducationLevel enum: BEGINNER, FUNDAMENTAL, INTERMEDIATE, EXPERT

  **사용자 제공 시스템 프롬프트 원문**:
  - 이전 세션에서 사용자가 제공한 persona별 시스템 프롬프트 4종을 PromptService에 상수로 추가
  - 각 persona 프롬프트의 정확한 내용은 `docs/ai-developer-guide.md` §4-11 참조

  **Acceptance Criteria**:

  **Agent-Executed QA Scenarios:**

  ```
  Scenario: 컴파일 성공 확인
    Tool: Bash
    Steps:
      1. ./gradlew spotlessApply compileJava --no-daemon
      2. Assert: "BUILD SUCCESSFUL"
    Expected Result: 컴파일 성공

  Scenario: 개인화 프롬프트 적용 확인 (서버 테스트)
    Tool: Bash
    Preconditions: 서버 실행 중, OPENAI_API_KEY 설정, 테스트 유저의 persona가 설정되어 있음
    Steps:
      1. TOKEN=$(curl -s -X POST http://localhost:8080/login \
           -H "Content-Type: application/json" \
           -d '{"username":"admin","password":"admin1234!"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
      2. curl -s -X POST http://localhost:8080/scenes/1/conversation/messages \
           -H "Authorization: Bearer $TOKEN" \
           -H "Content-Type: application/json" \
           -d '{"content":"이 부품이 뭐야?","references":[]}'
      3. Assert: HTTP 200, response has "answer" field (AI가 persona 톤으로 응답)
    Expected Result: AI 응답이 해당 persona의 말투/스타일을 반영
    Evidence: curl 응답 캡처

  Scenario: persona null인 유저 — 기본 프롬프트 fallback
    Tool: Bash
    Preconditions: persona가 null인 테스트 유저 존재
    Steps:
      1. 해당 유저로 로그인 → JWT 획득
      2. POST /scenes/1/conversation/messages 전송
      3. Assert: HTTP 200 (에러 아님, 기본 프롬프트로 정상 응답)
    Expected Result: NullPointerException 없이 정상 응답
  ```

  **Commit**: YES
  - Message: `feat(prompt): 페르소나 기반 시스템 프롬프트 개인화 추가`
  - Files: `PromptService.java`, `ConversationService.java`
  - Pre-commit: `./gradlew spotlessApply compileJava --no-daemon`

---

- [ ] 2. 퀴즈 임베딩 채점 (#8)

  **What to do**:

  **2-A. QuizRepository 생성** (`repository/QuizRepository.java`):
  - `JpaRepository<SceneRanksQuiz, Long>`
  - 별도 커스텀 메서드 불필요 (findById 기본 제공)

  **2-B. QuizChoiceRepository 생성** (`repository/QuizChoiceRepository.java`):
  - `JpaRepository<SceneRanksQuizChoice, Long>`
  - `List<SceneRanksQuizChoice> findBySceneRanksQuizIdOrderByOrderIndex(Long quizId)` — 보기 목록 조회

  **2-C. QuizUserProgressRepository 생성** (`repository/QuizUserProgressRepository.java`):
  - `JpaRepository<QuizUserProgress, Long>`
  - `Optional<QuizUserProgress> findByUserIdAndSceneInformationId(Long userId, Long sceneId)` — 유저별 진행도 조회

  **2-D. EmbeddingService 생성** (`service/EmbeddingService.java`):
  - OpenAI Embeddings API 호출 (`POST /v1/embeddings`, model: `text-embedding-3-small`)
  - RestClient 사용 (OpenAiConfig의 기존 RestClient 빈 재사용 또는 동일 패턴으로 구성)
  - 입력: 두 텍스트 (사용자 답변, 정답)
  - 출력: cosine similarity (double)
  - cosine similarity 계산 로직 직접 구현 (벡터 내적 / 노름 곱)

  **2-E. QuizDto 생성** (`dto/QuizDto.java`):
  - `GradeRequest`: `@NotBlank String answer`
  - `GradeResponse`: `boolean correct`, `double score`, `String correctAnswer` (INPUT 타입일 때만 score 유의미, SELECT는 1.0 or 0.0)

  **2-F. EmbeddingDto 추가** (`dto/OpenAiDto.java`에 추가 또는 별도 파일):
  - `EmbeddingRequest`: `String input`, `String model`
  - `EmbeddingResponse`: `List<EmbeddingData> data` → `EmbeddingData`: `List<Double> embedding`

  **2-G. QuizGradingService 생성** (`service/QuizGradingService.java`):
  - `GradeResponse grade(Long quizId, String userAnswer, User user)`:
    1. SceneRanksQuiz 조회 (없으면 QUIZ_NOT_FOUND 에러)
    2. quiz.type이 SELECT인 경우:
       - `quiz.getAnswer().equalsIgnoreCase(userAnswer)` 로 정답 비교
       - score = 정답이면 1.0, 오답이면 0.0
    3. quiz.type이 INPUT인 경우:
       - EmbeddingService로 userAnswer와 quiz.answer의 cosine similarity 계산
       - score >= 0.8 이면 correct = true
    4. QuizUserProgress 업데이트:
       - 기존 progress가 없으면 새로 생성
       - `lastQuizId` 업데이트
       - `totalQuestions` + 1
       - correct면 `success` + 1, 아니면 `failure` + 1
       - isComplete 판단 로직: 해당 scene의 전체 퀴즈 수와 비교 (필요 시)
    5. GradeResponse 반환

  **2-H. QuizController 생성** (`controller/QuizController.java`):
  - `@RestController`, `@RequestMapping("/scenes/{sceneId}/quiz")`
  - `@PostMapping("/{quizId}/grade")`:
    - `@AuthenticationPrincipal CustomUserDetails userDetails`
    - `@PathVariable Long sceneId`, `@PathVariable Long quizId`
    - `@Valid @RequestBody GradeRequest request`
    - User 로드 → QuizGradingService.grade() 호출 → GradeResponse 반환

  **2-I. CommonErrorCode에 에러코드 추가**:
  - `QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다")`
  - `EMBEDDING_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "임베딩 API 호출 실패")`

  **Must NOT do**:
  - 퀴즈 CRUD (생성/수정/삭제/목록) 엔드포인트 만들지 않는다 — 채점만
  - 벡터 DB나 RAG 인프라 구축하지 않는다
  - SceneRanksQuiz, SceneRanksQuizChoice, QuizUserProgress 엔티티의 @Table/@Column 변경하지 않는다
  - 퀴즈 완료 여부(isComplete)로 재채점을 거부하지 않는다 — 항상 허용
  - QuizUserProgress에 새 필드 추가하지 않는다

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 새 서비스 3개 + 컨트롤러 1개 + 리포지토리 3개 + DTO 다수 생성. 가장 큰 태스크.
  - **Skills**: [`git-master`]
    - `git-master`: 커밋 관리

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Task 1 완료 후)
  - **Blocks**: None
  - **Blocked By**: Task 0

  **References**:

  **Pattern References**:
  - `src/main/java/com/blaybus/backend/service/OpenAiService.java` — RestClient 기반 OpenAI API 호출 패턴. EmbeddingService도 동일 패턴으로 구현. `@Value`로 설정값 주입, retry 로직
  - `src/main/java/com/blaybus/backend/config/OpenAiConfig.java` — RestClient 빈 설정. baseUrl, Bearer token, timeout 설정 패턴
  - `src/main/java/com/blaybus/backend/controller/ConversationController.java:42-44` — `@AuthenticationPrincipal CustomUserDetails` + `findUser()` 인증 패턴. QuizController도 동일 패턴 사용
  - `src/main/java/com/blaybus/backend/dto/OpenAiDto.java` — record-of-records DTO 패턴. EmbeddingDto도 동일 스타일
  - `src/main/java/com/blaybus/backend/exception/CommonErrorCode.java` — 에러코드 enum 패턴. QUIZ_NOT_FOUND, EMBEDDING_API_ERROR 추가

  **API/Type References**:
  - `src/main/java/com/blaybus/backend/domain/quiz/SceneRanksQuiz.java` — 퀴즈 엔티티. `targetPurpose`, `type`(QuizType), `question`, `answer` 필드. `@ManyToOne SceneInformation`
  - `src/main/java/com/blaybus/backend/domain/quiz/SceneRanksQuizChoice.java` — 보기 엔티티. `orderIndex`, `content`. `@ManyToOne SceneRanksQuiz`
  - `src/main/java/com/blaybus/backend/domain/quiz/QuizUserProgress.java` — 진행도 엔티티. `lastQuizId`, `totalQuestions`, `success`, `failure`, `isComplete`, `solveTime`. FK: User, SceneInformation
  - `src/main/java/com/blaybus/backend/domain/quiz/QuizType.java` — enum: SELECT, INPUT
  - `src/main/java/com/blaybus/backend/security/CustomUserDetails.java` — 인증 사용자 정보

  **External References**:
  - OpenAI Embeddings API: `POST https://api.openai.com/v1/embeddings` — request: `{"input": "text", "model": "text-embedding-3-small"}`, response: `{"data": [{"embedding": [0.1, 0.2, ...]}]}`
  - application.yml의 `openai.embedding-model: text-embedding-3-small` — 이미 설정됨

  **Acceptance Criteria**:

  **Agent-Executed QA Scenarios:**

  ```
  Scenario: 컴파일 성공 확인
    Tool: Bash
    Steps:
      1. ./gradlew spotlessApply compileJava --no-daemon
      2. Assert: "BUILD SUCCESSFUL"
    Expected Result: 컴파일 성공

  Scenario: SELECT 퀴즈 채점 (정답)
    Tool: Bash
    Preconditions: 서버 실행 중, DB에 SELECT 타입 퀴즈 존재 (id=1, answer="A")
    Steps:
      1. TOKEN 획득 (로그인)
      2. curl -s -w "\n%{http_code}" -X POST http://localhost:8080/scenes/1/quiz/1/grade \
           -H "Authorization: Bearer $TOKEN" \
           -H "Content-Type: application/json" \
           -d '{"answer":"A"}'
      3. Assert: HTTP 200
      4. Assert: response.correct == true
      5. Assert: response.score == 1.0
    Expected Result: 정답 판정, score 1.0
    Evidence: curl 응답 캡처

  Scenario: SELECT 퀴즈 채점 (오답)
    Tool: Bash
    Preconditions: 동일 퀴즈
    Steps:
      1. curl -s -X POST http://localhost:8080/scenes/1/quiz/1/grade \
           -H "Authorization: Bearer $TOKEN" \
           -H "Content-Type: application/json" \
           -d '{"answer":"Z"}'
      2. Assert: HTTP 200
      3. Assert: response.correct == false
      4. Assert: response.score == 0.0
    Expected Result: 오답 판정, score 0.0

  Scenario: INPUT 퀴즈 채점 (임베딩 기반)
    Tool: Bash
    Preconditions: 서버 실행 중, OPENAI_API_KEY 설정, DB에 INPUT 타입 퀴즈 존재
    Steps:
      1. curl -s -X POST http://localhost:8080/scenes/1/quiz/2/grade \
           -H "Authorization: Bearer $TOKEN" \
           -H "Content-Type: application/json" \
           -d '{"answer":"기어는 회전력을 전달하는 부품입니다"}'
      2. Assert: HTTP 200
      3. Assert: response.score is numeric (0.0 ~ 1.0)
      4. Assert: response has "correct" field (boolean)
    Expected Result: 임베딩 기반 유사도 점수 반환

  Scenario: 존재하지 않는 퀴즈 채점 시도
    Tool: Bash
    Steps:
      1. curl -s -w "\n%{http_code}" -X POST http://localhost:8080/scenes/1/quiz/99999/grade \
           -H "Authorization: Bearer $TOKEN" \
           -H "Content-Type: application/json" \
           -d '{"answer":"test"}'
      2. Assert: HTTP 404
    Expected Result: QUIZ_NOT_FOUND 에러

  Scenario: 빈 답변으로 채점 시도
    Tool: Bash
    Steps:
      1. curl -s -w "\n%{http_code}" -X POST http://localhost:8080/scenes/1/quiz/1/grade \
           -H "Authorization: Bearer $TOKEN" \
           -H "Content-Type: application/json" \
           -d '{"answer":""}'
      2. Assert: HTTP 400 (validation error)
    Expected Result: @NotBlank 검증 실패
  ```

  **Commit**: YES
  - Message: `feat(quiz): 퀴즈 채점 엔드포인트 및 임베딩 기반 스코어링 추가`
  - Files: QuizController, QuizGradingService, EmbeddingService, QuizRepository, QuizChoiceRepository, QuizUserProgressRepository, QuizDto, EmbeddingDto/OpenAiDto 추가, CommonErrorCode 수정
  - Pre-commit: `./gradlew spotlessApply compileJava --no-daemon`

---

- [ ] 3. 토큰 사용량 모니터링 (#9)

  **What to do**:

  **3-A. OpenAiDto.java에 Usage record 추가**:
  - `ResponsesResponse` record에 `Usage usage` 필드 추가
  - `record Usage(int input_tokens, int output_tokens, int total_tokens) {}` 중첩 record 추가
  - Jackson이 자동으로 `usage` JSON 필드를 파싱하도록 함

  **3-B. OpenAiService.java 수정**:
  - Micrometer `MeterRegistry` 주입 (`@RequiredArgsConstructor` 또는 생성자)
  - `Counter` 2개 정의:
    - `openai.tokens.input` — input token 누적
    - `openai.tokens.output` — output token 누적
  - `chat()` 메서드에서 응답 파싱 후 usage 추출:
    ```java
    if (response.usage() != null) {
        Counter.builder("openai.tokens.input")
            .description("OpenAI input tokens consumed")
            .register(meterRegistry)
            .increment(response.usage().input_tokens());
        Counter.builder("openai.tokens.output")
            .description("OpenAI output tokens consumed")
            .register(meterRegistry)
            .increment(response.usage().output_tokens());
    }
    ```
  - Embedding API 토큰도 동일하게 카운트 (EmbeddingService에서도 usage 추출 + 별도 카운터):
    - `openai.tokens.embedding.input` — embedding input token 누적

  **3-C. EmbeddingService.java 수정** (Task 2에서 생성한 파일):
  - EmbeddingResponse에 `Usage usage` 필드 추가
  - MeterRegistry 주입 + embedding 토큰 카운터 추가

  **Must NOT do**:
  - 토큰 사용량을 DB에 저장하지 않는다
  - 대시보드/UI를 만들지 않는다
  - Rate limiting을 구현하지 않는다
  - 유저별/대화별 토큰 분리 추적하지 않는다 — 전체 aggregate 카운터만
  - 비용 계산 로직 추가하지 않는다

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: DTO 필드 추가 + Micrometer 카운터 3줄 추가. 매우 작은 변경.
  - **Skills**: [`git-master`]
    - `git-master`: 커밋 관리

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Task 2 완료 후 — EmbeddingService에 의존)
  - **Blocks**: None
  - **Blocked By**: Task 0, Task 2 (EmbeddingService)

  **References**:

  **Pattern References**:
  - `src/main/java/com/blaybus/backend/service/OpenAiService.java` — chat() 메서드에서 ResponsesResponse 파싱 위치. usage 추출 코드 삽입 지점
  - `src/main/java/com/blaybus/backend/dto/OpenAiDto.java` — ResponsesResponse record. Usage 중첩 record 추가 위치

  **API/Type References**:
  - `build.gradle` — `implementation 'io.micrometer:micrometer-registry-prometheus'` 이미 포함
  - `src/main/resources/application.yml` — `management.endpoints.web.exposure.include: prometheus, health, info` 이미 설정

  **External References**:
  - OpenAI Responses API usage 응답 형식: `{"usage": {"input_tokens": 100, "output_tokens": 50, "total_tokens": 150}}`
  - OpenAI Embeddings API usage 응답 형식: `{"usage": {"prompt_tokens": 10, "total_tokens": 10}}`
  - Micrometer Counter 사용법: `Counter.builder("name").register(registry).increment(amount)`

  **Acceptance Criteria**:

  **Agent-Executed QA Scenarios:**

  ```
  Scenario: 컴파일 성공 확인
    Tool: Bash
    Steps:
      1. ./gradlew spotlessApply compileJava --no-daemon
      2. Assert: "BUILD SUCCESSFUL"
    Expected Result: 컴파일 성공

  Scenario: Checkstyle 통과 확인 (최종 게이트)
    Tool: Bash
    Steps:
      1. ./gradlew checkstyleMain --no-daemon
      2. Assert: "BUILD SUCCESSFUL"
    Expected Result: 코드 스타일 준수

  Scenario: AI 호출 후 Prometheus 메트릭 확인
    Tool: Bash
    Preconditions: 서버 실행 중, OPENAI_API_KEY 설정
    Steps:
      1. curl -s -X POST http://localhost:8080/scenes/1/chat \
           -H "Content-Type: application/json" \
           -d '{"message":"테스트"}'
      2. Assert: HTTP 200 (AI 호출 성공)
      3. curl -s http://localhost:8080/actuator/prometheus | grep "openai_tokens"
      4. Assert: 출력에 "openai_tokens_input_total" 포함
      5. Assert: 출력에 "openai_tokens_output_total" 포함
      6. Assert: 값이 0보다 큼
    Expected Result: Prometheus 메트릭에 토큰 카운터 노출, 값 > 0
    Evidence: Prometheus 메트릭 출력 캡처

  Scenario: usage 필드 없는 응답 처리 (에러 안 남)
    Tool: Bash (코드 레벨 확인)
    Steps:
      1. OpenAiService 코드에서 `if (response.usage() != null)` null 체크 존재 확인
      2. grep -n "usage() != null" src/main/java/com/blaybus/backend/service/OpenAiService.java
      3. Assert: 매칭 라인 존재
    Expected Result: null safety 확인
  ```

  **Commit**: YES
  - Message: `feat(monitoring): OpenAI 토큰 사용량 Micrometer 메트릭 추가`
  - Files: OpenAiDto.java, OpenAiService.java, EmbeddingService.java
  - Pre-commit: `./gradlew spotlessApply compileJava --no-daemon`

---

## Commit Strategy

> **Branch**: `ai-feat` (develop에서 생성)
> **커밋 규칙**: prefix(scope): 한 문장 한국어

| After Task | Message | Key Files | Verification |
|------------|---------|-----------|--------------|
| 0 | `refactor(ai): 새 엔티티 패키지 구조에 맞춰 AI 코드 마이그레이션` | 전체 AI 코드 + entity 수정 | `./gradlew compileJava` |
| 1 | `feat(prompt): 페르소나 기반 시스템 프롬프트 개인화 추가` | PromptService, ConversationService | `./gradlew compileJava` |
| 2 | `feat(quiz): 퀴즈 채점 엔드포인트 및 임베딩 기반 스코어링 추가` | Quiz 관련 전체 신규 파일 | `./gradlew compileJava` |
| 3 | `feat(monitoring): OpenAI 토큰 사용량 Micrometer 메트릭 추가` | OpenAiDto, OpenAiService, EmbeddingService | `./gradlew checkstyleMain` |

---

## Success Criteria

### Verification Commands
```bash
# 전체 빌드 확인
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew spotlessApply build -x test -x checkstyleTest --no-daemon
# Expected: BUILD SUCCESSFUL

# Checkstyle 확인
./gradlew checkstyleMain --no-daemon
# Expected: BUILD SUCCESSFUL

# OLD 엔티티 잔재 확인
ls src/main/java/com/blaybus/backend/domain/{Conversation,Message,Reference,Component}.java 2>&1
# Expected: No such file or directory (4건)

# OLD import 잔재 확인
grep -r "import com.blaybus.backend.domain.Conversation;" src/main/java/
grep -r "import com.blaybus.backend.domain.Message;" src/main/java/
grep -r "import com.blaybus.backend.domain.Component;" src/main/java/
grep -r "import com.blaybus.backend.domain.Reference;" src/main/java/
# Expected: 결과 없음

# Prometheus 메트릭
curl -s http://localhost:8080/actuator/prometheus | grep openai_tokens
# Expected: openai_tokens_input_total, openai_tokens_output_total 라인 존재
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] `./gradlew build -x test -x checkstyleTest` 성공
- [ ] `./gradlew checkstyleMain` 성공
- [ ] OLD 엔티티 파일 0개
- [ ] 대화 엔드포인트 (POST /scenes/{sceneId}/conversation/messages) 정상 동작
- [ ] 퀴즈 채점 엔드포인트 (POST /scenes/{sceneId}/quiz/{quizId}/grade) 정상 동작
- [ ] Prometheus 메트릭에 토큰 카운터 노출
- [ ] 4개 커밋 완료 on `ai-feat` branch (migration, personalization, quiz, monitoring)
