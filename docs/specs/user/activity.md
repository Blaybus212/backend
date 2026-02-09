---
title: "활동 기록 기능 명세 (Activity Spec)"
type: "spec"
status: "active"
last_updated: "2026-02-09"
author: "김채원(stellaa223@gmail.com)"
related_components:
  ["ActivityController", "ActivityService", "ActivityResponse", "UserGrassRepository"]
tags: ["user", "activity", "grass", "streak"]
trigger_intent:
  - "사용자 활동 기록(잔디) API를 구현하거나 수정할 때"
  - "연속 학습 횟수(streak) 로직을 확인하거나 수정할 때"
  - "월간 활동 요약 데이터를 수정할 때"
---

# 📝 활동 기록 기능 명세

## 1. Overview

사용자의 매일 활동 성과를 점수로 환산하여 기록하고, 이를 월 단위 Heatmap(잔디) 형태로 시각화하기 위한 데이터를 제공한다.
또한 현재 기준의 연속 학습 횟수(Streak)를 관리한다.

## 2. API 명세

### Endpoint

```
GET /my/activity
```

### Headers

```
Authorization: Bearer ${accessToken}
```

### Response Body

| 필드명             | 타입   | 설명                                                   |
| :----------------- | :----- | :----------------------------------------------------- |
| `streak`           | number | 연속 학습 횟수 (오늘 기준)                              |
| `solvedQuizCount`  | number | 이번 달 맞춘 퀴즈 문항 수                              |
| `cells`            | object | 날짜별 활동 데이터 (Key: "YYYY-MM-DD")                  |
| `cells.{date}.score` | number | 해당 일자의 활동 총 점수                               |
| `cells.{date}.level` | number | 점수에 따른 레벨 (0~4)                                 |

**점수 산정 기준:**
- 하루에 부품 1개 열람 = 1점
- AI와의 대화 1번 = 1점
- 노트 작성 100자 이상 = 1점
- 퀴즈 하나 정답 = 1점

**레벨 정의:**
- 0: 0회
- 1: 1~10회
- 2: 11~20회
- 3: 21~30회
- 4: 31~50회 이상

**Response 예시:**

```json
{
  "streak": 3,
  "solvedQuizCount": 40,
  "cells": {
    "2026-02-09": { "score": 15, "level": 2 },
    "2026-02-08": { "score": 10, "level": 1 },
    "2026-02-07": { "score": 5, "level": 1 },
    "2026-02-01": { "score": 25, "level": 3 }
  }
}
```

## 3. 동작 로직 (Business Logic)

### Streak 계산 로직

- 오늘 날짜의 활동 데이터(`UserGrass`)가 존재하는 경우, 해당 데이터의 `streak` 값을 사용한다.
- 오늘 날짜의 기록이 없는 경우, 가장 최근의 활동 기록을 조회한다.
- 최근 기록이 '어제'인 경우, 어제의 `streak` 값을 그대로 노출한다 (오늘 활동을 완료하면 +1 될 예정이라는 의미).
- 그보다 오래전인 경우 `streak`은 0으로 초기화된다.

## 4. 관련 파일

| 파일                     | 역할                                           |
| :----------------------- | :--------------------------------------------- |
| `ActivityController.java`| 활동 기록 API 엔드포인트 정의                  |
| `ActivityService.java`   | 월간 활동 집계 및 Streak 계산 로직             |
| `ActivityResponse.java`  | API 응답 DTO                                   |
| `CellResponse.java`      | 일별 상세 정보 DTO                             |
| `UserGrass.java`         | 일별 활동 점수 및 Streak 저장 엔티티           |
| `UserGrassRepository.java`| 기간별 활동 데이터 조회 권한                   |

## 5. 🤖 AI Guidelines (Instructions)

1. **인증 필수**: 이 API는 JWT 인증이 필수이며, `CustomUserDetails`를 통해 사용자를 식별한다.
2. **Streak 유지**: 오늘 기록이 없어도 어제까지의 기록이 유효하다면 Streak을 유지하여 사용자에게 동기를 부여한다.
3. **날짜 키 형식**: `cells` 맵의 키는 반드시 `"YYYY-MM-DD"` 형식을 준수해야 한다.
