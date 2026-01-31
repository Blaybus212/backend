---
title: Docker 기반 컨테이너 환경 구축 및 오케스트레이션 전략
type: adr
status: accepted
tags: [devops, docker, infrastructure, deployment]
trigger_intent: 컨테이너 기반 배포/개발 환경 구성의 결정 배경을 파악할 때 이 문서를 참고한다.
---

# ADR-003: Docker 기반 컨테이너 환경 구축 및 오케스트레이션 전략

## Status

Accepted

## Date

2026-01-31

## Context

Google Cloud Platform (GCP) 등 클라우드 환경 배포 시 환경 설정 비용을 절감하기 위해 컨테이너 기반의 배포 전략이 필요합니다.
현재 로컬 개발 환경은 직접 실행(Binary Execution) 방식을 유지하지만, 배포 환경을 위한 컨테이너 표준이 요구됩니다.
이 과정에서 빌드 효율성, 보안(기밀 정보 관리), 그리고 서비스 실행 순서 보장에 대한 기준을 수립해야 했습니다.

## Decision

**우리는 Docker와 Docker Compose를 사용하여 애플리케이션과 데이터베이스를 컨테이너화하고 오케스트레이션하기로 결정했습니다.**

구체적인 전략은 다음과 같습니다:

1.  **Multi-stage Build 도입**:
    - Build Stage(Gradle)와 Runtime Stage(Slim JDK)를 분리하여 최종 이미지 용량을 최적화하고 빌드 도구 의존성을 제거합니다.

2.  **Dockerfile의 역할 분리**:
    - 단일 Dockerfile 대신 `Dockerfile.app` (애플리케이션 전용), `Dockerfile.db` (데이터베이스 전용) 등으로 명시적으로 파일명을 분리하여 관리합니다.
    - 이는 추후 각 컨테이너 별 설정이 복잡해질 경우를 대비하고 단일 책임 원칙을 준수하기 위함입니다.

3.  **환경 변수를 통한 기밀 정보 관리**:
    - DB 비밀번호 등 민감 정보는 Dockerfile이나 소스 코드에 하드코딩하지 않습니다.
    - `docker-compose.yml`에서 `${ENV_VAR}` 문법을 사용하여, 실행 시점(Runtime)에 외부(.env 파일 또는 CI/CD Secret)로부터 주입받습니다.

4.  **Healthcheck 기반의 실행 순서 제어**:
    - 단순 `depends_on`은 컨테이너의 시작(Running)만 보장하므로, DB가 초기화되기 전에 앱이 연결을 시도하여 실패하는 문제가 발생할 수 있습니다.
    - DB 컨테이너에 `pg_isready`를 이용한 `healthcheck`를 설정하고, 앱 컨테이너는 `condition: service_healthy` 조건을 통해 DB가 완전히 준비된 후 시작되도록 강제합니다.

## Trade-off Analysis

### 1. Docker Compose & Multi-stage Build (선택됨)

**장점 (Pros):**

- **환경 일관성**: 로컬, 개발, 운영 환경의 차이를 최소화.
- **배포 효율**: 최적화된 이미지 사이즈로 빠른 배포 가능.
- **안정성**: `healthcheck`를 통해 서비스 간 실행 순서와 의존성을 보장.

**단점 (Cons):**

- **복잡도**: Dockerfile 및 Compose 설정에 대한 관리 비용 발생.
- **빌드 시간**: 초기 Layer 캐싱이 없을 경우 빌드 시간이 소요될 수 있음.

### 2. 로컬 직접 실행 (Binary Execution) (기각됨)

**장점 (Pros):**

- **단순함**: 별도 설정 없이 IDE에서 바로 실행 가능.

**단점 (Cons):**

- **환경 파편화**: 개발자 PC마다 JDK 버전, DB 설치 여부가 달라 문제 발생 가능.
- **배포 차이**: 로컬은 직접 실행, 운영은 컨테이너 실행일 경우 동작 차이 발생 위험.

## Consequences

- Google Cloud 등 배포 환경에서 `docker-compose` 또는 이에 준하는 컨테이너 실행 방식을 사용할 준비가 되었습니다.
- 로컬 개발 시 개별 환경에 따라서 환경변수 `DB_USERNAME`, `DB_PASSWORD` 등을 설정해야 합니다. (vscode 기준 `${projectRoot}/.env`)
- 모든 인프라 변경 사항은 Docker 관련 설정 코드(`Dockerfile.*`, `docker-compose.yml`)로 관리(IaC)됩니다.
