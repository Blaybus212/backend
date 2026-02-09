---
title: "Scene Sync API 명세 (Scene Sync API Spec)"
type: "spec"
status: "active"
last_updated: "2026-02-09"
author: "강민준(joonamin44@gmail.com)"
related_components:
  [
    "SceneAssemblyController",
    "SceneAssemblyService",
    "SceneSyncDto",
    "ComponentStateDto",
    "Alignment",
    "UserScene",
  ]
tags: ["scene", "sync", "api", "3d", "camera", "alignment"]
---

# 🔄 Scene Sync API 명세

## 1. 개요

사용자가 3D Scene 내의 컴포넌트들을 재배치하거나 카메라 설정(`lookAt`)을 변경했을 때, 이를 실시간으로 서버에 저장하고 동기화하기 위한 API입니다.

## 2. API 명세

### Endpoint

```http
PUT /scenes/{sceneId}/sync
```

### Request Headers

```http
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
```

### Path Parameters

| 필드명    | 타입 | 설명              |
| :-------- | :--- | :---------------- |
| `sceneId` | Long | 동기화할 Scene ID |

### Request Body (`SceneSyncDto`)

| 필드명       | 타입   | 설명                                                   |
| :----------- | :----- | :----------------------------------------------------- |
| `components` | Array  | 재배치된 컴포넌트 리스트 (`ComponentStateDto[]`)       |
| `lookAt`     | Object | 카메라 설정 (position, target 등 자유 형식의 JSON Map) |

#### ComponentStateDto

| 필드명     | 타입   | 설명                                               |
| :--------- | :----- | :------------------------------------------------- |
| `nodeName` | String | GLTF 내 노드의 고유 이름                           |
| `matrix`   | Array  | 4x4 변환 행렬 (16개의 Double 값으로 구성된 리스트) |

### Request Example (JSON)

```json
{
  "components": [
    {
      "nodeName": "Arm_gear1",
      "matrix": [
        0.966, 0.009, 0.255, 0, 0, 0.999, -0.035, 0, -0.255, 0.034, 0.966, 0,
        10.5, -2.1, 5.0, 1
      ]
    },
    {
      "nodeName": "Leg1",
      "matrix": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0.126, 1]
    }
  ],
  "lookAt": {
    "position": [15.0, 10.0, 20.0],
    "target": [0.0, 0.0, 0.0],
    "fov": 45.0
  }
}
```

## 3. 동작 로직

1. **사용자 환경 확인**: `@AuthenticationPrincipal`을 통해 현재 요청을 보낸 사용자의 식별자(userId)를 확보합니다.
2. **카메라 설정 업데이트**: `lookAt` 데이터가 포함되어 있다면 `UserScene` 테이블의 해당 사용자/Scene 레코드를 찾아 업데이트(혹은 생성)합니다.
3. **컴포넌트 정렬 업데이트**: `components` 리스트를 순회하며 각 `nodeName`에 해당하는 `Alignment` 데이터를 조회합니다.
   - 기존 데이터가 있으면 변환 행렬(`transformMatrix`)을 업데이트합니다.
   - 기존 데이터가 없으면 새로운 `Alignment` 레코드를 생성하여 저장합니다.

## 4. 응답 구조

- **성공**: `200 OK` (내용 없음)
- **실패**:
  - `401 Unauthorized`: 인증 토큰이 누락되거나 유효하지 않은 경우
  - `404 Not Found`: 존재하지 않는 `sceneId`인 경우
  - `400 Bad Request`: JSON 형식이 올바르지 않거나 필수 값이 누락된 경우
