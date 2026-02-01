---
title: 환경별(Profile) 로깅 전략 수립
type: adr
status: accepted
tags: [logging, devops, observability, gcp]
trigger_intent: 환경별(Dev/Prod) 로깅 전략의 차이와 결정 이유를 이해할 때 이 문서를 참고한다.
---

# ADR-006: 환경별(Profile) 로깅 전략 수립

## Status

Accepted

## Date

2026-02-01

## Context

애플리케이션의 로그는 디버깅과 운영 모니터링의 핵심 요소입니다. 하지만 개발(Dev/Local) 환경과 운영(Prod) 환경은 로그 활용 방식과 요구사항이 서로 다릅니다.

1.  **개발 환경 (Dev/Local)**:
    - 개발자가 즉시 로그 파일을 열어보거나 검색할 수 있어야 합니다.
    - IDE나 텍스트 에디터에서의 접근성이 중요합니다.
    - 로그가 너무 많이 쌓여 디스크를 잠식하거나 관리가 안 되는 "파일 폭탄" 문제를 예방해야 합니다.

2.  **운영 환경 (Prod)**:
    - 서버에 접속해서 파일을 읽는 것은 보안상, 관리상 좋지 않습니다.
    - 컨테이너 환경(Docker)의 표준인 **Stdout**을 준수해야 합니다.
    - 중앙 집중화된 로그 저장소(GCP Cloud Logging)로 전송되어 영구 보관 및 검색이 가능해야 합니다.

따라서, 단일 로깅 전략이 아닌 **프로파일별(Profile-specific) 이원화된 전략**이 필요합니다.

## Decision

우리는 환경별로 다음과 같이 다른 로깅 전략을 적용합니다.

### 1. Dev Profile: Hybrid Rolling File Strategy

- **저장 방식**: 로컬 파일 시스템(`logs/` 디렉토리)에 로그를 저장합니다.
- **파일 관리 전략**: **Time + Size Hybrid Rolling Policy**를 적용합니다.
  - **파일명**: `logs/backend-dev.log` (현재 로그)
  - **Archiving**: 날짜가 바뀌거나, 파일 크기가 **10MB**를 초과하면 `logs/archived/` 폴더로 이동 및 인덱싱(`%i`)하여 저장합니다.
  - **Retention**: 최근 **30일** 간의 로그만 보관하고 오래된 로그는 자동 삭제합니다.
- **이유**: 개발 편의성을 극대화하고, 로컬 디스크 용량 부족 문제를 방지하기 위함입니다.

### 2. Prod Profile: Stdout & Ops Agent Strategy

- **저장 방식**: 별도 파일 생성 없이 **Console (Standard Output)**로만 출력합니다. (Spring Boot 기본값)
- **수집 방식**: **GCP Ops Agent**를 활용합니다.
  - VM 레벨에 설치된 Ops Agent가 Docker 컨테이너의 stdout을 수집합니다.
  - **필수 설정**: VM의 `/etc/google-cloud-ops-agent/config.yaml`에 `docker_logs` 수집 설정이 포함되어야 합니다.
- **이유**: Twelve-Factor App 윈칙의 11챕터의 log원칙을 준수하며, 관리형 서비스(Cloud Logging)를 통해 검색, 필터링, 영구 보관 기능을 비용 효율적으로 활용하기 위함입니다.

## Consequences

### Positive (장점)

- **개발 생산성**: 개발자는 로컬에서 익숙한 파일 형태로 로그를 쉽게 다룰 수 있습니다.
- **운영 안정성**: 프로덕션 서버의 디스크가 로그 파일로 가득 차는 문제를 원천 차단합니다. (Stdout은 Docker가 관리, Cloud Logging은 클라우드 스토리지 사용)
- **리소스 효율**: 불필요한 파일 I/O(Prod)나 무거운 수집기 실행(Dev)을 방지합니다.

### Negative (단점)

- **설정 복잡도**: `application-dev.yml`(파일 설정)과 Ops Agent Config(수집 설정)를 각각 관리해야 합니다.
- **환경 차이**: 로컬에서 보던 로그 파일이 실서버에는 없으므로 혼란을 겪을 수 있습니다. (클라우드 모니터링 도구로 접근하도록 유도)

## Implementation Details

- **Dev Config (`application-dev.yml`)**:
  ```yaml
  logging:
    file:
      name: logs/backend-dev.log
    logback:
      rollingpolicy:
        file-name-pattern: logs/archived/backend-dev-%d{yyyy-MM-dd}.%i.log
        max-file-size: 10MB
        max-history: 30
  ```
- **Prod Config**: 별도 설정 없음 (`logging.file` 설정 제거 -> Stdout 출력).
- **Ops Agent Config** (GCP VM): `logging.receivers.docker_logs` 타입 설정 추가.
  - **동작 원리**: Ops Agent는 `/var/lib/docker/containers/*/*.log` 경로에 있는 Docker JSON 로그 파일들을 실시간으로 Tailing합니다. 여기서 수집된 로그는 컨테이너 메타데이터(Image, Container Name 등)와 함께 Cloud Logging으로 전송됩니다.
  - **Status**: 해당 설정은 2026-02-01 기준으로 운영 VM에 **수동으로 적용 완료**되었습니다.
