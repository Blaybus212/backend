---
title: Prometheus와 Grafana를 활용한 애플리케이션 모니터링 전략
type: adr
status: accepted
tags: [monitoring, observability, devops, prometheus, grafana]
trigger_intent: 모니터링 시스템 구축 결정 배경과 구성 요소를 파악할 때 이 문서를 참고한다.
---

# ADR-005: Prometheus와 Grafana를 활용한 애플리케이션 모니터링 전략

## Status

Accepted

## Date

2026-02-01

## Context

우리의 "Blaybus" 백엔드 애플리케이션은 GCP Compute Engine 상에서 Docker Container 기반으로 운영되고 있습니다.
서비스의 안정성을 확보하고 성능 병목을 조기에 발견하기 위해서는 애플리케이션 상태(JVM 메모리, CPU, 스레드 등)와 트래픽(HTTP 요청 수, 응답 시간, 에러율)을 실시간으로 관측할 수 있는 모니터링 시스템이 필요합니다.

고려해야 할 요구사항은 다음과 같습니다:

1.  **비용 효율성**: 초기 단계이므로 과도한 비용이 발생하는 유료 SaaS(Datadog 등)나 매니지드 서비스보다는 자체 구축 또는 무료 티어 내 활용이 가능한 솔루션이 선호됩니다.
2.  **표준 호환성**: Spring Boot 생태계에서 널리 쓰이는 표준적인 방법을 따라야 합니다.
3.  **배포 용이성**: 기존 Docker Compose 기반 배포 파이프라인에 쉽게 통합되어야 합니다.
4.  **가시성**: 별도의 복잡한 설정 없이 배포 즉시 주요 지표를 시각화할 수 있어야 합니다.

## Decision

우리는 **Spring Boot Actuator**, **Prometheus**, **Grafana**를 조합한 모니터링 스택을 도입하기로 결정했습니다.

### 1. Metrics Exposure: Spring Boot Actuator

- 애플리케이션의 내부 메트릭을 노출하기 위해 `spring-boot-starter-actuator` 라이브러리를 사용합니다.
- `micrometer-registry-prometheus`를 추가하여 메트릭을 Prometheus가 수집 가능한 포맷으로 변환합니다.
- 보안 설정을 통해 `/actuator/prometheus` 및 `/health-check` 엔드포인트에 대한 접근을 허용합니다.
- Tomcat 내장 서버의 상세 통계(Thread Pool 등)를 수집하기 위해 `server.tomcat.mbeanregistry.enabled` 옵션을 활성화합니다.

### 2. Metrics Collection: Prometheus

- 시계열 데이터베이스(TSDB)인 Prometheus를 사용하여 주기적으로 애플리케이션의 엔드포인트를 호출(Scrape)하고 데이터를 저장합니다.
- Docker Compose 내에서 별도의 컨테이너로 실행하며, `prometheus.yml`을 통해 수집 대상을 설정합니다.

### 3. Visualization: Grafana

- 수집된 데이터를 시각화하기 위해 Grafana를 사용합니다.
- **Provisioning 자동화**: 매번 수동으로 설정하는 번거로움을 없애기 위해, 컨테이너 구동 시 `datasource.yml`과 `dashboard.yml`을 통해 Prometheus 데이터소스 연결과 대시보드 생성을 자동으로 수행합니다.
- 기본 대시보드로 Spring Boot 전용 대시보드(JVM, HTTP, Tomcat 지표 포함)를 내장하여 즉각적인 모니터링 환경을 제공합니다.

## Consequences

### Positive (장점)

- **비용 절감**: 오픈소스 솔루션을 자체 호스팅하므로 별도의 라이선스 비용이 발생하지 않습니다.
- **풍부한 생태계**: Spring Boot와 Prometheus/Grafana는 사실상의 업계 표준으로, 참고 자료가 많고 문제 해결이 용이합니다.
- **즉시 사용 가능 (Out-of-the-box)**: 자동 프로비저닝 설정을 통해 배포와 동시에 대시보드를 사용할 수 있어 운영 편의성이 증대됩니다.
- **확장성**: 추후 다른 서비스(DB, Node Exporter 등)가 추가되더라도 Prometheus 설정만 변경하여 통합 모니터링이 가능합니다.

### Negative (단점)

- **인프라 관리 부담**: 관리형 서비스와 달리 모니터링 서버(Prometheus/Grafana)의 가용성과 스토리지 용량을 직접 관리해야 합니다.
- **리소스 사용**: 모니터링 컨테이너가 애플리케이션과 같은 호스트에서 실행될 경우, 리소스를 일부 점유할 수 있습니다. (현재 트래픽 규모에서는 무시할 만한 수준으로 판단됨)

## Implementation Details

- **Docker Compose**: `prometheus` 및 `grafana` 서비스 추가 및 볼륨 마운트 설정.
- **Grafana Dashboard**: 'Spring Boot Statistics (Universal)' 대시보드 JSON을 커스터마이징(한글 툴팁 추가, 호환성 수정)하여 적용.
- **Security**: Spring Security 설정에서 Actuator 엔드포인트 허용 (`.requestMatchers("/actuator/**").permitAll()`).
