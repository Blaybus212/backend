---
title: "학습 중인 Scene 조회 기능 명세 (Learning Scenes Spec)"
type: "spec"
status: "active"
last_updated: "2026-02-08"
author: "김채원(stellaa223@gmail.com)"
related_components:
  ["SceneController", "SceneService", "UserSceneRepository", "SceneResponse"]
tags: ["scene", "learning", "home"]
trigger_intent:
  - "홈 화면의 '학습 중인 오브젝트' API를 수정할 때"
  - "Scene 진척도 혹은 인기 여부 판단 로직을 변경할 때"
---

# 📝 학습 중인 Scene 조회 기능 명세

## 1. Overview

홈 화면에서 로그인한 사용자가 최근에 학습(접속)했던 Scene 목록을 최대 3개까지 노출한다.
각 Scene은 제목, 이미지, 진척도, 인기 여부 등의 정보를 포함한다.

## 2. API 명세

### Endpoint

```
GET /my/recent/scenes
```

### Headers

```
Authorization: Bearer ${accessToken}
```

### Response Body

| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `scenes` | array | 학습 중인 Scene 객체 배열 (최대 3개) |
| `scenes[].id` | string | Scene 식별자 |
| `scenes[].title` | string | Scene 제목 (한글) |
| `scenes[].engTitle` | string | Scene 제목 (영어) |
| `scenes[].category` | string | 카테고리 (예: 기계공학) |
| `scenes[].imageUrl` | string | 썸네일 이미지 URL |
| `scenes[].progress` | number | 학습 진척도 (0~100) |
| `scenes[].popular` | boolean | 인기 Scene 여부 |
| `scenes[].lastAccessedAt` | string | 최근 접속 시간 (ISO 8601 형식) |

**Response 예시:**

```json
{
  "scenes": [
    {
      "id": "1",
      "title": "로봇 팔",
      "engTitle": "Robot Arm",
      "category": "기계공학",
      "imageUrl": "https://example.com/image.png",
      "progress": 35,
      "popular": true
    }
  ]
}
```

## 3. 동작 로직 (Business Logic)

### 데이터 조회 및 정렬
1. **사용자 식별**: `CustomUserDetails`를 통해 요청자의 `userId`를 획득한다.
2. **목록 필터링 및 정렬**: `UserScene` 테이블에서 해당 사용자의 기록을 `lastAccessedAt` 내림차순(최신순)으로 정렬한다.
3. **개수 제한**: 상위 3개의 레코드만 가져온다. (성능을 위해 `SceneInformation`과 Fetch Join 수행)

### 필드 데이터 결정 규칙
- **progress (진척도)**: 현재는 **35**로 하드코딩되어 있으며, 추후 퀴즈 풀이 결과 등과 연동될 예정이다. (TODO 기입됨)
- **popular (인기 여부)**: `SceneInformation`의 `participantsCount`가 **5 이상**인 경우 `true`로 판단한다.

## 4. 관련 파일

| 파일 | 역할 |
| :--- | :--- |
| `SceneController.java` | GET /scenes 엔드포인트 정의 |
| `SceneService.java` | 학습 목록 조회 및 데이터 가공 로직 |
| `UserSceneRepository.java` | 최근 학습 Scene 3개 조회 쿼리 |
| `SceneResponse.java` | 응답 데이터 형식 정의 (DTO) |
| `UserScene.java` | 사용자별 Scene 접속 정보를 담는 엔티티 |
| `SceneInformation.java` | Scene 기본 정보 엔티티 |

## 5. 🤖 AI Guidelines (Instructions)

1. **인기 여부 수정**: `popular` 기준이 되는 `participantsCount` 임계치는 비즈니스 요구에 따라 변경될 수 있으며, `SceneService`에서 관리한다.
2. **진척도 고도화**: `progress` 필드를 실제 데이터 기반으로 수정할 때는 `QuizUserProgress` 혹은 관련 통계 테이블을 참조하도록 `SceneService`를 업데이트해야 한다.
3. **성능 최적화**: 목록 조회 시 반드시 `SceneInformation`을 Fetch Join하여 N+1 문제를 방지해야 한다.
