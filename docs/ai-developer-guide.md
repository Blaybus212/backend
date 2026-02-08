---
title: SIMVEX AI 담당자 업무 가이드
type: guide
status: accepted
tags: [ai, openai, gpt, embedding, architecture]
trigger_intent: AI 어시스턴트 기능의 설계, 구현, 제약 조건을 파악할 때 이 문서를 참고한다.
---

# SIMVEX AI 담당자 업무 가이드

## 1. 기술 스택

| 항목 | 기술 | 용도 |
|------|------|------|
| 대화 생성 | **GPT-5-mini** (OpenAI) | 어시스턴트 응답 + 누적 요약 동시 생성, 오답 설명 |
| 임베딩 | **text-embedding-3-small** (OpenAI) | 주관식 퀴즈 채점 (유사도 비교) |
| API 방식 | **Responses API** (`/v1/responses`) | Chat Completions가 아닌 최신 API |
| 응답 형식 | **Structured Output** (`strict: true`) | JSON 스키마 100% 준수 강제 — 파싱 실패 방지 |
| HTTP 클라이언트 | **Spring RestClient** | Java 공식 SDK 미지원 → REST 직접 호출 |
| API 키 | 주최측 제공 | 예산 캡 존재, 비정상 사용 시 정지 가능 |

---

## 2. 제약 조건

| 제약 | 출처 | 영향 |
|------|------|------|
| **컨텍스트 윈도우 4K 이하** | REQ-AI-003 (주최측 요구) | 프롬프트 압축 필수, running summary 전략 |
| **API 예산 캡** | 주최측 | 불필요한 호출 최소화, 토큰 모니터링 |
| **부품 메타데이터 미제공** | Q&A 미팅 | PM이 직접 조사 → Component 테이블에 입력 |
| **AI 정확도 < UX** | Q&A 미팅 | 완벽한 답변보다 자연스러운 학습 흐름이 중요 |
| **시스템 프롬프트 내부 설정** | Q&A 미팅 | 사용자에게 노출 없이 Scene별 자동 적용 |
| **페르소나 자유** | Q&A 미팅 | 교수, 선배, 전문가 등 팀 재량 |
| **10일 MVP** | 해커톤 | 완벽보다 동작하는 것 우선 |

---

## 3. 담당 영역

```
AI 담당 업무
│
├─ A. 대화 시스템 ──────────── (핵심)
│   ├─ A-1. OpenAI API 연동 (Structured Output)
│   ├─ A-2. 시스템 프롬프트 설계
│   ├─ A-3. @부품 태깅 → Component DB 조회 → 프롬프트 주입
│   ├─ A-4. Running Summary 방식 4K 컨텍스트 관리
│   └─ A-5. Conversation / Message CRUD & 대화 복원
│
├─ B. 퀴즈 채점 ────────────── (부가)
│   └─ B-1. 주관식 답안 Embedding 유사도 채점
│
└─ C. 토큰 모니터링 ────────── (운영)
    └─ C-1. API 사용량 추적
```

---

## 4. 대화 시스템 상세

### 4-1. 핵심 설계 원칙

데이터 저장과 LLM 요청은 **역할이 분리**됩니다:

```
Message 테이블          → 프론트 대화 UI 복원용 (전체 원문 보관)
Conversation.summary    → LLM 요청용 (압축된 누적 맥락)
```

- **Message**: 사용자가 화면을 떠났다 돌아왔을 때 전체 대화 이력을 복원하기 위한 원본 저장소.
  매 턴마다 USER 메시지와 ASSISTANT 메시지가 append됩니다.
- **summary** (구 `running_summary`): LLM에게 보내는 맥락. 매 턴마다 갱신되는 누적 요약본으로,
  4K 제한 내에서 전체 대화 맥락을 압축 전달합니다.

### 4-2. 전체 흐름

```
[Frontend]                        [Backend]                        [OpenAI]
    │                                │                                │
    │  POST /scenes/{sceneId}/conversation/messages                   │
    │    body: {                     │                                │
    │      content: "@모터 이건      │                                │
    │               어떻게 작동해?", │                                │
    │      references: [             │                                │
    │        { componentId: 3 }      │                                │
    │      ]                         │                                │
    │    }                           │                                │
    │ ──────────────────────────────→│                                │
    │                                │                                │
    │                                │  1. componentId:3 →            │
    │                                │     Component DB 조회          │
    │                                │                                │
    │                                │  2. Conversation에서           │
    │                                │     running_summary 로드       │
    │                                │     (첫 대화면 null)           │
    │                                │                                │
    │                                │  3. 프롬프트 조립              │
    │                                │     System Prompt              │
    │                                │     + Component 메타데이터     │
    │                                │     + running_summary          │
    │                                │     + 현재 User Query          │
    │                                │     + "응답과 요약을 함께 줘"  │
    │                                │                                │
    │                                │  4. GPT-5-mini 호출            │
    │                                │     (Structured Output)        │
    │                                │  ──────────────────────────→   │
    │                                │  ←──────────────────────────   │
    │                                │                                │
    │                                │  응답:                         │
    │                                │  {                             │
    │                                │    "answer": "DC 모터는...",   │
    │                                │    "summary": "사용자가        │
    │                                │     모터 원리를 질문. DC 모터  │
    │                                │     의 전자기 유도 원리 설명"  │
    │                                │  }                             │
    │                                │                                │
    │                                │  5. Message 저장               │
    │                                │     - USER 메시지 append       │
    │                                │     - ASSISTANT 메시지 append  │
    │                                │     - Reference 저장           │
    │                                │                                │
    │                                │  6. Conversation               │
    │                                │     .running_summary 갱신      │
    │                                │                                │
    │  ←──────────────────────────── │                                │
    │    { sender: "ASSISTANT",      │                                │
    │      content: "DC **모터**는   │                                │
    │      전자기 유도 원리로..." }  │                                │
```

### 4-3. Running Summary 동작

```
턴 1:
  → 입력: running_summary = null
  → LLM 응답: { answer: "...", summary: "모터 작동 원리 질문에 답변" }
  → DB: Conversation.running_summary = "모터 작동 원리 질문에 답변"

턴 2:
  → 입력: running_summary = "모터 작동 원리 질문에 답변"
  → LLM 응답: { answer: "...", summary: "모터 원리와 RPM 범위를 논의" }
  → DB: Conversation.running_summary = "모터 원리와 RPM 범위를 논의"

턴 3:
  → 입력: running_summary = "모터 원리와 RPM 범위를 논의"
  → LLM 응답: { answer: "...", summary: "모터-기어 연결 방식과 기어비 계산까지 확장" }
  → DB: Conversation.running_summary = "모터-기어 연결 방식과 기어비 계산까지 확장"

  ...누적 요약이 매 턴마다 갱신됨
```

**이 방식의 이점:**

| 항목 | 효과 |
|------|------|
| 토큰 고정 | 요약본 크기가 일정 → 대화 100턴이든 4K를 넘지 않음 |
| 별도 요약 호출 없음 | 매 턴 응답에 요약이 포함 → 추가 API 호출 0회 |
| 비용 절감 | 전체 대화 이력을 보내지 않으므로 입력 토큰 절약 |
| 구현 단순 | 요약 트리거 타이밍 고민 불필요 — 항상 동일 구조 |

### 4-4. OpenAI Responses API & Structured Output

> **참고**: https://platform.openai.com/docs/guides/structured-outputs

#### API 엔드포인트

OpenAI **Responses API** (`/v1/responses`)를 사용합니다. (Chat Completions API가 아님)

```
POST https://api.openai.com/v1/responses
```

#### 요청 형식

```json
{
  "model": "gpt-5-mini",
  "input": [
    { "role": "system", "content": "(시스템 프롬프트)" },
    { "role": "user", "content": "(running_summary + component context + user query)" }
  ],
  "text": {
    "format": {
      "type": "json_schema",
      "name": "assistant_response",
      "strict": true,
      "schema": {
        "type": "object",
        "properties": {
          "answer":  { "type": "string" },
          "summary": { "type": "string" }
        },
        "required": ["answer", "summary"],
        "additionalProperties": false
      }
    }
  }
}
```

- `strict: true` — 스키마 100% 준수 보장 (필수)
- `additionalProperties: false` — 정의된 필드만 반환 (필수)

#### 응답 형식

```json
{
  "answer": "DC 모터는 전자기 유도 원리를 이용하여...",
  "summary": "사용자가 DC 모터의 작동 원리를 질문. 전자기 유도와 로렌츠 힘 기반의 회전 메커니즘 설명"
}
```

- `answer` → 프론트엔드에 그대로 전달
- `summary` → Conversation.running_summary에 저장 (다음 요청에 사용)

#### 엣지 케이스 처리

| 상황 | 감지 방법 | 처리 |
|------|-----------|------|
| **안전 거부 (refusal)** | `response.status === "incomplete"`, `refusal` 필드 존재 | 프론트에 "답변할 수 없는 질문입니다" 메시지 반환 |
| **토큰 초과 (응답 잘림)** | `incomplete_details.reason === "max_output_tokens"` | 재시도 또는 에러 응답 |
| **API 오류** | HTTP 4xx/5xx | 재시도 (최대 2회) 후 에러 응답 |

#### Spring Boot 연동

공식 OpenAI SDK는 Python/JavaScript만 지원. Java에서는 **Spring RestClient로 직접 호출**.
프로젝트에 `spring-boot-starter-restclient` 의존성이 이미 존재합니다.

```java
// OpenAI API 호출 예시 (Spring Boot)
RestClient restClient = RestClient.builder()
    .baseUrl("https://api.openai.com/v1")
    .defaultHeader("Authorization", "Bearer " + apiKey)
    .defaultHeader("Content-Type", "application/json")
    .build();

String responseBody = restClient.post()
    .uri("/responses")
    .body(requestPayload)
    .retrieve()
    .body(String.class);

// JSON 파싱 → answer, summary 추출
AssistantResponse parsed = objectMapper.readValue(responseBody, AssistantResponse.class);
```

API 키는 `.env` 파일로 관리하며 (ADR-009), `application.yml`에서 환경 변수로 참조합니다.

```yaml
# application.yml
openai:
  api-key: ${OPENAI_API_KEY}
```

### 4-5. 프론트엔드 → 백엔드 요청 형식

```json
POST /scenes/{sceneId}/conversation/messages

{
  "content": "@모터 이건 어떻게 작동해?",
  "references": [
    { "componentId": 3 }
  ]
}
```

프론트엔드에서 `@부품이름` 태깅 시, 해당 Component의 ID를 함께 전송합니다.

> **Stateless 테스트용 엔드포인트**: `POST /scenes/{sceneId}/chat` — 인증 불필요, DB 저장 없음.
> 요청: `{ "message": "모터가 뭐야?" }` / 응답: `{ "answer": "..." }`

### 4-6. Component 정보 주입 방식

프론트엔드가 `componentId`를 명시적으로 보내주므로, **임베딩 검색(RAG) 없이 DB 직접 조회**로 처리합니다.

```
프론트 태그: @모터 → componentId: 3

백엔드:
  SELECT name, description, texture, usage
  FROM component
  WHERE id = 3

결과:
  name: "DC 모터"
  description: "직류 전류를 이용한 회전 모터"
  texture: "구리, 알루미늄"
  usage: "회전력 생성, 동력 전달"

→ 이 정보를 프롬프트의 Component Context 영역에 삽입
```

**RAG를 사용하지 않는 이유:**
- 3D 뷰어에서 부품을 클릭/태그하는 구조라 참조 대상이 항상 명확
- componentId가 프론트에서 넘어오므로 검색 불필요
- DB 직접 조회가 더 단순하고, 정확하고, 비용 0

### 4-7. 4K 토큰 예산 배분

```
총 4,096 토큰

┌─ System Prompt ──────────────────────────┐  ~400 토큰
│ - 역할: SIMVEX 공학 학습 어시스턴트       │
│ - 규칙: 한국어, 부품명 강조, 간결한 답변  │
│ - 페르소나: (팀 결정)                    │
│ - Scene 개요: {title}, {category}        │
│ - Structured Output 지시                 │
└──────────────────────────────────────────┘

┌─ Component Context ──────────────────────┐  ~300 토큰
│ (태그된 부품만 DB 조회하여 삽입)          │
│ - DC 모터: 구리/알루미늄, 회전력 생성     │
│ - 평기어: 강철, 동력 전달                │
└──────────────────────────────────────────┘

┌─ Running Summary ────────────────────────┐  ~500 토큰
│ (이전 대화의 누적 요약)                   │
│ "모터 원리와 RPM 논의 후,                │
│  기어 연결 방식으로 확장"                 │
└──────────────────────────────────────────┘

┌─ Current Query ──────────────────────────┐  ~100 토큰
│ USER: @피니언 이건 여기서 어떤 역할이야?  │
└──────────────────────────────────────────┘

                        응답 여유 (answer + summary): ~2,800 토큰
```

Running Summary 방식이므로 대화 이력 전체를 보낼 필요가 없어 응답 여유가 넉넉합니다.

### 4-8. 메시지 저장 형식

매 턴마다 USER + ASSISTANT 메시지가 Message 테이블에 **append** 됩니다.

```
Message 테이블 (지속적 append):
  ┌─ 턴 1 ─────────────────────────────────────────────┐
  │ id:1  sender: "USER"       content: "@모터 작동 원리?"  posted_at: T1 │
  │ id:2  sender: "ASSISTANT"  content: "DC 모터는..."      posted_at: T2 │
  ├─ 턴 2 ─────────────────────────────────────────────┤
  │ id:3  sender: "USER"       content: "@기어 연결 방식?"  posted_at: T3 │
  │ id:4  sender: "ASSISTANT"  content: "기어는 모터에..."  posted_at: T4 │
  ├─ 턴 3 ─────────────────────────────────────────────┤
  │ id:5  sender: "USER"       content: "@피니언 역할?"     posted_at: T5 │
  │ id:6  sender: "ASSISTANT"  content: "피니언은..."       posted_at: T6 │
  └────────────────────────────────────────────────────┘

Reference 테이블:
  message_id: 1, component_id: 3 (모터)
  message_id: 3, component_id: 7 (기어)
  message_id: 5, component_id: 12 (피니언)

Conversation 테이블:
  running_summary: "모터 원리, RPM, 기어 연결, 피니언 역할까지 논의"
  (매 턴마다 갱신)
```

### 4-9. Conversation 테이블

```
| column   | type           | description                                      |
|----------|----------------|--------------------------------------------------|
| id       | BIGINT (PK)    | 자체 식별자                                       |
| user_id  | BIGINT (FK)    | 사용자 아이디                                     |
| scene_id | BIGINT (FK → SceneInformation) | 소속된 SceneInformation 식별자       |
| summary  | TEXT (nullable) | LLM 누적 요약 (첫 대화면 null, 매 턴 갱신)       |
```

> **컬럼명 변경**: 기존 설계 `running_summary` → 신규 스키마 `summary`.
> 현재 코드(`Conversation.java`)는 `running_summary`로 구현되어 있으므로, 스키마 확정 후 컬럼명 동기화 필요.

### 4-10. 대화 이력 복원

사용자가 다른 화면에 갔다가 돌아왔을 때, **Message 테이블의 전체 원문**으로 대화 UI를 복원합니다.

```sql
-- 1. 해당 사용자 + Scene의 Conversation 조회
SELECT * FROM conversation
WHERE user_id = ? AND scene_id = ?;

-- 2. 해당 Conversation의 전체 메시지 로드 (UI 복원용)
SELECT * FROM message
WHERE conversation_id = ?
ORDER BY posted_at ASC;

-- 3. 각 메시지의 참조 부품 조회
SELECT * FROM reference
WHERE message_id IN (?);
```

프론트엔드는 이 데이터로 전체 대화 UI를 복원하고,
백엔드는 새 메시지 요청 시 `Conversation.summary`만 LLM에 전달합니다.

### 4-11. User 개인화 프롬프트 설계

신규 스키마에서 User 테이블에 AI 전용 컨텍스트 필드가 추가됨:

| 필드 | 타입 | 프롬프트 활용 |
|------|------|---------------|
| `persona` | VARCHAR(50) | 답변 어조 결정: "senior"(선배), "professor"(교수), "friend"(친구), "assistant"(조수) |
| `education_level` | VARCHAR(50) | 답변 난이도 조절: BEGINNER, FUNDAMENTAL, INTERMEDIATE, EXPERT |
| `specialized_in` | VARCHAR(150) | 이미 아는 분야 스킵. 쉼표 구분 텍스트 (예: "IT 개발,자동차 부품") |

시스템 프롬프트에 다음과 같이 주입:

```
## 사용자 프로필
- 답변 어조: {persona} (선배처럼 / 교수처럼 / 친구처럼 / 조수처럼)
- 학습 수준: {education_level} → 해당 수준에 맞춰 용어 난이도 조절
- 이미 잘 아는 분야: {specialized_in} → 해당 분야는 기초 설명 생략
```

> **구현 위치**: `PromptService.buildSystemPrompt()` 에서 User 정보를 받아 프롬프트 동적 조립.
> 현재는 고정 프롬프트만 사용 중 → User 엔티티 조회 후 개인화 적용 필요.

---

## 5. 주관식 퀴즈 채점

### 5-1. 방식: Embedding 유사도 + GPT 설명 하이브리드

```
[입력]
  Quiz.answer:  "고정자, 회전자, 정류자"       ← 정답 기준
  사용자 답안:   "스테이터, 로터, 브러시"        ← 학생 제출

[1차 판정: Embedding 유사도]
  정답 embed = text-embedding-3-small("고정자, 회전자, 정류자")
  학생 embed = text-embedding-3-small("스테이터, 로터, 브러시")
  similarity = cosine_similarity(정답, 학생)

[분기]
  similarity ≥ 0.9  → 즉시 정답 (GPT 호출 불필요)
  similarity ≤ 0.5  → 즉시 오답 → GPT로 설명 생성
  0.5 < sim < 0.9   → GPT-5-mini로 정밀 판정 + 설명

[오답 시 GPT 설명]
  프롬프트: "정답: X, 학생 답안: Y.
            오답 이유를 간결하게 설명하세요."
  → 설명 요약을 프론트에 반환
```

**이 하이브리드 방식의 이점:**
- 명확한 정답/오답은 Embedding만으로 처리 → API 비용 절감
- 애매한 답안만 GPT 정밀 판정 → 정확도 확보
- 오답 시 반드시 설명 제공 → REQ-QUIZ-003 충족

### 5-2. 정확도 기준

- REQ-QUIZ-003: **80% 이상이면 정답, 미만이면 오답 + 설명**
- Embedding cosine similarity 0.8을 80% 정확도 기준으로 매핑
- 경계값(0.5~0.9)은 GPT가 0~100% 정확도를 직접 판정

### 5-3. QuizChoice 테이블 (신규)

객관식 퀴즈의 선택지를 별도 테이블로 관리:

```
| column      | type         | description       |
|-------------|--------------|-------------------|
| id          | BIGINT (PK)  | 선택지 식별자      |
| quiz_id     | BIGINT (FK)  | 소속된 퀴즈        |
| choice_key  | VARCHAR(5)   | "A", "B", "C", "D"|
| content     | VARCHAR(255) | 선택지 문구        |
| order_index | INT          | 렌더링 순서        |
```

> AI 채점은 `type = "INPUT"` (주관식)에만 적용. 객관식(`type = "SELECT"`)은 `Quiz.answer`와 직접 비교.

---

## 6. text-embedding-3-small 사용처 정리

| 용도 | 사용 여부 | 이유 |
|------|-----------|------|
| 대화 시 Component 검색 (RAG) | **사용 안 함** | 프론트에서 componentId를 명시적으로 전송하므로 DB 직접 조회 |
| 주관식 퀴즈 채점 | **사용** | 정답 vs 학생 답안 유사도 비교 |
| 대화 이력 검색 | **사용 안 함** | running_summary로 충분 |

---

## 7. 태스크 목록 & 일정

### 7-1. 우선순위별 태스크

| # | 태스크 | 근거 | 우선순위 | 상태 |
|---|--------|------|----------|------|
| 1 | OpenAI API 연동 (Structured Output) | 전제 조건 | 최상 | ✅ 완료 |
| 2 | Conversation / Message CRUD API | REQ-AI-001 | 최상 | ✅ 완료 |
| 3 | 시스템 프롬프트 설계 (기본) | REQ-AI-001, 002 | 최상 | ✅ 기본 구현 |
| 4 | @태그 → Component DB 조회 → 프롬프트 주입 | REQ-LEARN-006 | 상 | ✅ 완료 |
| 5 | Running Summary 기반 4K 컨텍스트 관리 | REQ-AI-003 | 상 | ✅ 완료 |
| 6 | 대화 이력 저장 (append) & 복원 | REQ-AI-003 | 상 | ✅ 완료 |
| 7 | User 개인화 프롬프트 반영 | REQ-AI-001 | 상 | ❌ 미구현 |
| 8 | 주관식 퀴즈 Embedding 채점 | REQ-QUIZ-003 | 상 | ❌ 미구현 |
| 9 | 토큰 사용량 모니터링 | 운영 (예산캡) | 중 | ❌ 미구현 |
| 10 | 취약점 퀴즈 생성 | 스키마 target_purpose | 낮음 | MVP 드롭 |

### 7-2. 남은 태스크 상세

```
#7  User 개인화 프롬프트 반영
    → User.persona, education_level, specialized_in을 시스템 프롬프트에 주입
    → §4-11 참고

#8  주관식 퀴즈 Embedding 채점
    → POST /quizzes/{quizId}/grade
    → text-embedding-3-small 유사도 + GPT 설명 하이브리드
    → QuizChoice 테이블 반영 필요 (신규 스키마)

#9  토큰 사용량 모니터링
    → OpenAI 응답의 usage 필드 로깅
    → 예산 초과 방지
```

---

## 8. 관련 요구사항 매핑

| 요구사항 ID | 설명 | AI 담당 태스크 |
|-------------|------|----------------|
| REQ-AI-001 | Scene 메타데이터 + 사물 리스트 + 이전 대화를 컨텍스트로 AI 답변 생성 | #1, #2, #3, #4, #5 |
| REQ-AI-002 | 부품 활용 사례 질문 시 정보 카드 데이터와 연동하여 답변 | #4 |
| REQ-AI-003 | 컨텍스트 윈도우 4K 이하 유지. 이전 대화 요약. 자동저장 | #5, #6 |
| REQ-LEARN-006 | @부품이름 태깅, 맥락 파악하여 강조 응답 | #4 |
| REQ-QUIZ-003 | 주관식 AI 채점: 정확도 80% 이상 정답, 미만 오답 + 설명 | #7 |
