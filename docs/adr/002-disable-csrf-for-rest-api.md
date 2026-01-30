---
title: REST API에서의 CSRF 보호 비활성화
type: adr
status: accepted
tags: [security, spring-security, csrf, rest-api]
trigger_intent: CSRF 보안 설정의 비활성화 사유와 보안 영향을 검토할 때 이 문서를 참고한다.
---

# ADR-002: REST API에서의 CSRF 보호 비활성화

## Status

Accepted

## Date

2026-01-31

## Context

[ADR-001: Spring Security 도입](./001-adopt-spring-security.md) 결정에 따라 Spring Security가 적용되었습니다.
기본적으로 Spring Security는 모든 요청에 대해 CSRF(Cross-Site Request Forgery) 보호를 활성화합니다.
그러나 본 서비스는 브라우저의 쿠키(Cookie)가 아닌 HTTP 헤더(Authorization)에 토큰을 담는 **Stateless REST API** 방식을 사용합니다.

## Decision

**REST API 엔드포인트에 대해 CSRF 보호 기능을 비활성화(`csrf.disable()`)하기로 결정했습니다.**

## Trade-off Analysis

### 1. 비활성화 (선택됨)

**이유:**

- **Stateless 구조**: 헤더 기반 인증(Bearer Token)은 브라우저가 자동으로 요청에 포함시키지 않으므로, CSRF 공격(사용자 몰래 요청 전송)이 성립하기 어렵습니다.
- **개발 효율성**: 클라이언트가 매 요청마다 CSRF 토큰을 관리하고 전송하는 오버헤드를 제거합니다.

### 2. 활성화 (기각됨)

**이유:**

- **불필요한 복잡도**: 쿠키를 사용하지 않는 환경에서 실질적인 보안 이득 없이 구현 복잡도만 증가합니다.

## Consequences

- `SecurityConfig`에서 `.csrf(AbstractHttpConfigurer::disable)` 설정을 유지합니다.
- **주의**: 향후 브라우저 기반의 세션/쿠키 인증이 도입된다면 이 정책은 즉시 재검토되어야 합니다.
