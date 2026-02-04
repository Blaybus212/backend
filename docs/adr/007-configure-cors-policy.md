---
title: CORS 정책 수립 및 환경별 적용전략
type: adr
status: accepted
tags: [security, cors, configuration, architecture]
trigger_intent: CORS 정책의 수립 배경과 환경별 적용 기준을 파악할 때 이 문서를 참고한다.
---

# ADR-007: CORS 정책 수립 및 환경별 적용전략

## Status

Accepted

## Date

2026-02-05

## Context

`com.blaybus.backend` 서비스는 프론트엔드와 백엔드가 분리된 아키텍처를 따르고 있습니다. 브라우저의 보안 정책인 SOP(Same-Origin Policy)로 인해, 다른 도메인(Origin) 간의 리소스 요청 시 CORS(Cross-Origin Resource Sharing) 설정이 필수적입니다.

특히 다음과 같은 요구사항을 고려해야 합니다.

1. **인증 정보 공유**: 로그인 세션, 쿠키, Authorization 헤더 등을 주고받기 위해 `Access-Control-Allow-Credentials: true` 설정이 필요합니다.
2. **보안성**: 개발 편의를 위해 모든 도메인(`*`)을 허용하는 것은 보안상 위험하며, `Allow-Credentials: true`와 함께 사용할 수 없습니다.
3. **환경별 차이**: 로컬 개발 환경(`localhost`), 테스트 환경, 운영 환경의 도메인이 서로 다릅니다.

## Decision

**우리는 `dev`와 `prod` 프로파일을 활용하여 환경별로 명시적인 Origin을 관리하고, Spring Security 설정을 통해 이를 적용하기로 결정했습니다.**

상세 전략은 다음과 같습니다:

1. **명시적 Origin 허용**:
   - 와일드카드(`*`) 사용을 금지하고, 신뢰할 수 있는 도메인만 명시적으로 허용합니다.
   - 이는 `Allow-Credentials: true` 설정이 요구하는 사양이기도 합니다.

2. **프로파일 기반 설정 분리**:
   - `application-dev.yml`: 로컬 개발을 위한 `http://localhost:3000` 등 허용.
   - `application-prod.yml`: 실제 운영 도메인(예: `https://blaybus.com`)만 허용.
   - 설정값은 `app.cors.allowed-origins` 프로퍼티로 관리합니다.

3. **엔드포인트별 정책 통일**:
   - `CorsConfigurationSource` Bean을 정의하여 모든 Security Filter Chain에 전역적으로 적용합니다.

## Trade-off Analysis

### 1. 명시적 목록 관리 (선택됨)

**장점 (Pros):**

- **보안 강화**: 허용되지 않은 출처에서의 접근을 원천 차단하여 CSRF 등의 공격 위험을 줄임.
- **표준 준수**: `Access-Control-Allow-Credentials` 사용 시의 브라우저 스펙 준수.
- **가시성**: 어떤 도메인이 시스템에 접근 가능한지 설정 파일만 보고 파악 가능.

**단점 (Cons):**

- **운영 비용**: 프론트엔드 도메인이 변경되거나 추가될 때마다 백엔드 설정 변경 및 배포가 필요함.
- **개발 번거로움**: 로컬 개발 시 디바이스 테스팅 등으로 Origin이 변경되면 설정을 수정해야 함.

### 2. 와일드카드 (\*) 허용 (기각됨)

**장점 (Pros):**

- **편의성**: 어떤 도메인에서든 접근 가능하므로 초기 개발이 빠름.

**단점 (Cons):**

- **보안 취약**: 악의적인 사이트에서도 API 호출이 가능해짐.
- **기능 제한**: `Allow-Credentials: true`를 사용할 수 없어 인증 기반 서비스에 부적합.

## Consequences

- 앞으로 프론트엔드 배포 URL이 생성되거나 변경될 때, 백엔드 담당자에게 해당 URL 등록을 요청해야 합니다.
- CORS 관련 에러 발생 시, 우선적으로 `application-*.yml` 파일의 `allowed-origins` 목록을 확인해야 합니다.
- CORS 설정이 `application-*.yml` 파일에 생성되었습니다. 이 파일이 없을 경우 test가 실패합니다.
