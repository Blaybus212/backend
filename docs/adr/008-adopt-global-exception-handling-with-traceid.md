---
title: 전역 예외 처리 및 TraceId 통합 전략
type: adr
status: accepted
tags: [exception-handling, observability, api-design, architecture]
trigger_intent: 예외 처리 구조와 TraceId 통합 배경을 파악할 때 이 문서를 참고한다.
---

# ADR-008: 전역 예외 처리 및 TraceId 통합 전략

## Status

Accepted

## Date

2026-02-05

## Context

REST API 서버에서 예외 처리는 다음과 같은 요구사항을 충족해야 합니다:

1. **프론트엔드 개발자 친화성**: 4xx 에러 발생 시 클라이언트 측 실수를 빠르게 파악하고 수정할 수 있도록 일관된 에러 응답 형식 제공
2. **에러 추적성**: 테스트 과정에서 대량의 로그가 쌓일 때, 특정 에러를 빠르게 식별하기 위한 TraceId 필요
3. **협업 효율성**: 프론트엔드 개발자와 백엔드 개발자가 TraceId를 기반으로 신속하게 소통
4. **중복 최소화**: 매번 예외마다 TraceId를 수동으로 추가하는 반복 작업 제거
5. **에러 코드 관리**: 시스템에서 발생 가능한 에러를 한눈에 파악하고 중복 유형 최소화

## Decision

**우리는 `@RestControllerAdvice` 기반의 전역 예외 처리 시스템을 구축하고, Micrometer Tracing의 TraceId를 모든 에러 응답에 자동으로 포함시키기로 결정했습니다.**

### 핵심 구성 요소

#### 1. GlobalExceptionHandler (`@RestControllerAdvice`)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Tracer tracer;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessExceptions(BusinessException ex) {
        return ResponseEntity
            .status(ex.getErrorCode().getHttpStatus())
            .body(ErrorResponse.of(ex.getErrorCode(), getTraceId()));
    }

    private String getTraceId() {
        return tracer.currentSpan() != null
            ? tracer.currentSpan().context().traceId()
            : "N/A";
    }
}
```

**역할**:

- 모든 Controller에서 발생하는 예외를 중앙에서 처리
- TraceId를 자동으로 주입하여 반복 코드 제거
- 일관된 에러 응답 형식 보장

#### 2. ErrorCode (Enum 기반 관리)

```java
public enum CommonErrorCode implements ErrorCode {
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "파라미터가 유효하지 않습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    // ...
}
```

**역할**:

- 발생 가능한 모든 에러를 리스트 형식으로 응집
- 중복 에러 유형 최소화
- HTTP 상태 코드와 메시지를 한 곳에서 관리

#### 3. ErrorResponse (표준화된 응답 구조)

```json
{
  "code": "CommonErrorCode.LOGIN_FAILED",
  "message": "아이디 또는 비밀번호가 일치하지 않습니다.",
  "traceId": "65f3a2b1c4d5e6f7",
  "errors": []
}
```

**역할**:

- 모든 에러 응답의 공통 틀 제공
- TraceId를 포함하여 로그 추적 가능
- Validation 에러 시 상세 필드 정보 제공

## Trade-off Analysis

### 1. @RestControllerAdvice vs @ControllerAdvice

**@RestControllerAdvice 선택 이유**:

- REST API 서버이므로 모든 응답이 JSON 형식
- `@ResponseBody`를 자동으로 적용하여 코드 간결화
- View 렌더링이 필요 없음

### 2. 커스텀 ErrorResponse vs Spring 기본 응답

**커스텀 ErrorResponse 선택 이유 (Pros)**:

- **일관성**: 모든 에러가 동일한 구조로 응답 (`code`, `message`, `traceId`, `errors`)
- **프론트엔드 친화성**: 에러 처리 로직을 단순화 (항상 같은 필드 파싱)
- **확장성**: 필요 시 추가 필드 (예: `timestamp`, `path`) 쉽게 추가 가능
- **TraceId 통합**: 자동으로 TraceId를 포함시켜 디버깅 효율 극대화

**Spring 기본 응답 사용 시 단점 (Cons)**:

- 에러마다 응답 구조가 다를 수 있음
- TraceId를 수동으로 추가해야 함 (반복 작업)
- 프론트엔드에서 에러 타입별로 다른 파싱 로직 필요

### 3. Enum 기반 ErrorCode vs 클래스 기반

**Enum 선택 이유 (Pros)**:

- **가시성**: 한 파일에서 모든 에러 코드 확인 가능
- **중복 방지**: 새 에러 추가 시 기존 에러와 중복 여부 즉시 파악
- **타입 안정성**: 컴파일 타임에 존재하지 않는 에러 코드 사용 방지
- **IDE 지원**: 자동 완성으로 사용 가능한 에러 코드 쉽게 탐색

**클래스 기반 사용 시 단점 (Cons)**:

- 에러 코드가 여러 파일에 분산되어 전체 파악 어려움
- 중복 에러 유형 발생 가능성 증가

### 4. TraceId 자동 주입 vs 수동 추가

**자동 주입 선택 이유 (Pros)**:

- **DRY 원칙**: 모든 예외 처리 지점에서 `getTraceId()` 호출 불필요
- **누락 방지**: 개발자가 실수로 TraceId를 빼먹는 경우 방지
- **유지보수성**: TraceId 추출 로직 변경 시 한 곳만 수정

**수동 추가 시 단점 (Cons)**:

- 매번 `ErrorResponse.of(..., getTraceId())` 형태로 반복 코드 작성
- 일부 예외에서 TraceId 누락 가능성

## Consequences

### Positive

- 프론트엔드 개발자가 에러 응답의 `traceId`를 백엔드 개발자에게 전달하면, 로그에서 해당 요청을 즉시 추적 가능
- 새로운 에러 추가 시 `CommonErrorCode`에 enum 값만 추가하면 됨
- 모든 Controller에서 일관된 에러 처리 보장 (개발자가 별도 처리 불필요)
- Validation 에러 발생 시 어떤 필드가 잘못되었는지 상세 정보 제공

### Negative

- 새로운 도메인별 에러가 필요할 경우 `ErrorCode` 인터페이스를 구현하는 별도 Enum 생성 필요
- TraceId가 없는 환경(로컬 개발 등)에서는 "N/A"로 표시됨

### Maintenance

- 에러 코드 추가 시 `CommonErrorCode` enum에 새 항목 추가
- 도메인별 에러가 많아지면 `UserErrorCode`, `OrderErrorCode` 등으로 분리 고려
- TraceId 추출 로직 변경 시 `GlobalExceptionHandler.getTraceId()` 메서드만 수정

## Related Files

- `src/main/java/com/blaybus/backend/exception/GlobalExceptionHandler.java`
- `src/main/java/com/blaybus/backend/exception/ErrorCode.java`
- `src/main/java/com/blaybus/backend/exception/CommonErrorCode.java`
- `src/main/java/com/blaybus/backend/exception/BusinessException.java`
- `src/main/java/com/blaybus/backend/dto/ErrorResponse.java`
