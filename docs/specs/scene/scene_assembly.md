---
title: "Scene Assembly API 명세 (Scene Assembly API Spec)"
type: "spec"
status: "active"
last_updated: "2026-02-09"
author: "강민준(joonamin44@gmail.com)"
related_components:
  [
    "SceneAssemblyController",
    "SceneAssemblyService",
    "SceneAssemblyDto",
    "SceneNodeDto",
    "Alignment",
    "Component",
  ]
tags: ["scene", "assembly", "api", "gltf", "export", "3d"]
---

# 🏗️ Scene Assembly API 명세

## 1. 개요

사용자가 3D Scene의 초기 조립 정보를 서버에 저장하기 위한 API입니다.

## 2. API 명세

### 2.1 Scene 조립 정보 저장

초기 씬 구성 정보(각 노드의 위치, 계층 구조 등)를 DB에 저장합니다.

- **Endpoint**: `POST /scenes/assembly`
- **Request Headers**:
  ```http
  Content-Type: application/json
  Authorization: Bearer {JWT_TOKEN}
  ```

#### Request Body (`SceneAssemblyDto`)

| 필드명  | 타입   | 설명                                |
| :------ | :----- | :---------------------------------- |
| `file`  | String | 에셋 경로 (예: `Drone/Drone.gltf`)  |
| `nodes` | Array  | 노드 정보 리스트 (`SceneNodeDto[]`) |

#### SceneNodeDto

| 필드명     | 타입   | 설명                                 |
| :--------- | :----- | :----------------------------------- |
| `name`     | String | 노드 이름 (예: `Arm_gear1`)          |
| `matrix`   | Array  | 4x4 변환 행렬 (16개의 Double 리스트) |
| `children` | Array  | 자식 노드의 인덱스 리스트 (필요 시)  |

#### Request Example

```json
{
  "file": "Drone/Drone.gltf",
  "nodes": [
    {
      "name": "Arm_gear1",
      "matrix": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1],
      "children": []
    }
  ]
}
```

## 3. 동작 로직

```mermaid
graph TD
    A[Client] -->|POST /assembly| B[SceneAssemblyController]
    B -->|saveAssembly| C[SceneAssemblyService]
    C -->|Save| D[(Alignment / Component DB)]
```

## 4. 관련 문서

- [Scene Sync API 명세](./scene_sync.md)
- [Scene Viewer ZIP 내보내기 명세](./viewer_export.md)

## 5. 초기 데이터 로딩 (Initial Data Loading)

### 5.1 개요

서버 구동 시점에 `DataLoader`를 통해 사전 정의된 메타데이터와 조립 설정 파일을 파싱하여 DB에 적재합니다.

### 5.2 데이터 소스 구조

- **Scene Metadata**: `src/main/resources/data/initial_scene_data.json`
  - 씬의 기본 정보 (Title, ID, Description, Asset Path 등) 정의
- **Component Metadata**: `src/main/resources/data/initial_component_data.json`
  - 씬에 종속된 부품들의 메타데이터 (Description, Texture, Usage 등) 정의
- **Assembly Config**: `src/main/resources/assets/{SceneName}/config/assembly_config.json`
  - 씬의 초기 조립 상태 (Matrix, 부품 배치) 정의
  - GLTF 생성 및 뷰어 로드 시 기준이 되는 **Base Configuration**

### 5.3 로딩 프로세스

1. **Scene 엔티티 생성**: `initial_scene_data.json`을 읽어 `SceneInformation` 테이블에 저장.
2. **Component 엔티티 생성**: `initial_component_data.json`을 읽어 각 씬에 속한 `Component` 테이블에 저장.
3. **Viewer 생성 시**: `assembly_config.json`을 로드하여 기본 조립 상태를 구성하고, 사용자별 `Alignment` 데이터를 병합(Merge)하여 최종 GLTF 생성.
