---
title: "Scene 랭킹 조회 기능 명세 (Scene Ranks Spec)"
type: "spec"
status: "active"
last_updated: "2026-02-08"
author: "Antigravity"
related_components:
  ["SceneController", "SceneService", "SceneStatisticsRepository", "SceneRankResponse"]
tags: ["scene", "ranking", "statistics", "popular"]
trigger_intent:
  - "Scene 랭킹 API를 구현하거나 수정할 때"
  - "인기 학습 오브젝트 순위 로직을 변경할 때"
  - "SceneStatistics 집계 시점 정책을 확인할 때"
---

# 📝 Scene 랭킹 조회 기능 명세

## 1. Overview

오늘 날짜 기준으로 사람들이 많이 학습한 Scene을 1위부터 5위까지 조회하는 기능.
카테고리별 필터링을 지원하며, 전날 대비 순위 변동 정보를 함께 제공한다.

## 2. API 명세

### Endpoint

```
GET /scenes/ranks
```

### Headers

```
Authorization: Bearer ${accessToken}
```

### Query Parameters

| 파라미터 | 타입   | 필수여부 | 설명                                          |
| :------- | :----- | :------- | :-------------------------------------------- |
| category | string | No       | Scene 카테고리 필터 (미지정 시 전체 카테고리) |

**허용 카테고리 값:**
- `robotics` - 로봇공학
- `automotive_engineering` - 자동차공학
- `aerospace_engineering` - 항공우주공학
- `manufacturing_engineering` - 제조공학

### Response Body

| 필드명              | 타입   | 설명                                      |
| :------------------ | :----- | :---------------------------------------- |
| `today`             | string | 현재 시각 (yyyy-MM-dd HH:mm 형식)         |
| `scenes`            | array  | 랭킹 정보 배열 (최대 5개)                 |
| `scenes[].id`       | string | Scene 식별자                              |
| `scenes[].rank`     | number | 현재 순위 (1~5)                           |
| `scenes[].title`    | string | Scene 제목 (한글)                         |
| `scenes[].engTitle` | string | Scene 제목 (영어)                         |
| `scenes[].rankDiff` | number | 순위 변동 (이전 랭킹 - 현재 랭킹 차이값) |

**Response 예시:**

```json
{
  "today": "2026-02-08 19:16",
  "scenes": [
    {
      "id": "1",
      "rank": 1,
      "title": "로봇 팔",
      "engTitle": "Robot Arm",
      "rankDiff": 2
    },
    {
      "id": "2",
      "rank": 2,
      "title": "자동차 엔진 4행정",
      "engTitle": "4-Stroke Engine Cycle",
      "rankDiff": -1
    }
  ]
}
```

## 3. 동작 로직 (Business Logic)

### 집계 시점 계산 (aggregatedTime)

랭킹 데이터는 매일 **07:00**를 기준으로 집계되며, 조회 시점에 따라 참조하는 집계 데이터가 달라진다:

- **현재 시각이 07:00 이후**: 어제 07:00 기준 집계 데이터 사용
- **현재 시각이 07:00 이전**: 그제 07:00 기준 집계 데이터 사용

**예시:**
- 2026-02-08 18:00 조회 → 2026-02-07 07:00 집계 데이터
- 2026-02-08 06:00 조회 → 2026-02-06 07:00 집계 데이터

### 데이터 조회 흐름

1. **집계 시점 계산**: 현재 시각을 기준으로 `aggregatedTime` 산출
2. **랭킹 데이터 조회**: `SceneStatistics` 테이블에서 해당 `aggregatedTime`과 `category`에 맞는 데이터를 `rank` 오름차순으로 조회 (LIMIT 5)
3. **응답 생성**: 조회된 데이터를 DTO로 변환하여 반환

### 순위 변동 계산 (rankDiff)

- `SceneStatistics` 테이블의 `difference` 필드에 사전 계산된 값 사용
- 배치 프로세스에서 전날 랭킹과 비교하여 계산됨
- 양수: 순위 상승, 음수: 순위 하락, 0: 변동 없음

## 4. 관련 파일

| 파일                             | 역할                                    |
| :------------------------------- | :-------------------------------------- |
| `SceneController.java`           | GET /scenes/ranks 엔드포인트 정의       |
| `SceneService.java`              | 랭킹 조회 및 집계 시점 계산 로직        |
| `SceneStatisticsRepository.java` | 랭킹 데이터 조회 쿼리 (JPQL)            |
| `SceneRankResponse.java`         | 응답 데이터 형식 정의 (DTO)             |
| `SceneStatistics.java`           | Scene 통계 스냅샷 엔티티                |
| `SceneInformation.java`          | Scene 기본 정보 엔티티                  |
| `DataLoader.java`                | 시연용 더미 랭킹 데이터 삽입 (초기화)   |

## 5. 🤖 AI Guidelines (Instructions)

1. **집계 시점 정책**: `aggregatedTime` 계산 로직은 `SceneService.calculateAggregatedTime()` 메서드에서 관리하며, 07:00 기준은 비즈니스 요구에 따라 변경될 수 있다.
2. **배치 프로세스 연동**: 현재 `DataLoader`에서 더미 데이터를 삽입하지만, 실제 운영에서는 별도의 배치 작업으로 `SceneStatistics` 테이블을 갱신해야 한다.
3. **성능 최적화**: 레포지토리 쿼리에서 `JOIN FETCH`와 `LIMIT 5`를 사용하여 N+1 문제를 방지하고 불필요한 데이터 조회를 제한한다.
4. **카테고리 필터**: `category` 파라미터가 null인 경우 전체 카테고리를 대상으로 조회하며, JPQL에서 동적 조건 처리를 수행한다.
