---
title: 토큰 기반 인증을 위한 Spring Security 도입
type: adr
status: accepted
tags: [security, architecture, spring-boot, authentication]
trigger_intent: 인증/인가 아키텍처의 기반 기술 선정 배경을 파악할 때 이 문서를 참고한다.
---

# ADR-001: 토큰 기반 인증을 위한 Spring Security 도입

## Status

Accepted

## Date

2026-01-31

## Context

`com.blaybus.backend` 서비스는 REST API 기반의 백엔드 시스템입니다.
안전하고 확장성 있는 인증(Authentication)과 인가(Authorization) 처리를 위해, 자체 구현(Custom) 방식과 표준 프레임워크 도입 간의 비교가 필요했습니다.

## Decision

**우리는 인증 및 인가 처리를 위해 Spring Security 프레임워크를 도입하기로 결정했습니다.**

## Trade-off Analysis

### 1. Spring Security 도입 (선택됨)

**장점 (Pros):**

- **표준화된 인가 처리**: `@PreAuthorize` 등 어노테이션 기반으로 비즈니스 로직과 보안 로직을 분리 가능.
- **검증된 보안성**: BCrypt, 보안 헤더, CORS 관리 등 검증된 보안 기능 즉시 사용.
- **전역 보안 컨텍스트**: `SecurityContextHolder`를 통한 사용자 정보 접근 용이성.
- **생태계 호환성**: OAuth2, LDAP 등 확장 용이.

**단점 (Cons):**

- **학습 곡선**: 프레임워크의 동작 방식 이해 필요.
- **초기 설정 복잡도**: 단순 구현 대비 설정 코드가 다소 많음.

### 2. 직접 구현 (Custom) (기각됨)

**장점 (Pros):**

- **단순함**: 초기 구현이 직관적.
- **제어권**: 불필요한 기능 없이 필요한 것만 구현 가능.

**단점 (Cons):**

- **보안 취약점 위험**: 알려진 보안 공격에 대한 방어를 직접 구현해야 함.
- **확장성 부족**: 요구사항 변경 시 구조 재설계 필요.

## Consequences

- 모든 보안 설정은 `SecurityConfig` 클래스 등을 통해 관리됩니다.
- 구체적인 보안 정책(예: CSRF, CORS)은 별도의 ADR로 관리될 수 있습니다.
