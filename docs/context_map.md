# 🗺️ Project Context Map

> **AI에게:** 이 파일은 프로젝트의 네비게이션 지도입니다. 사용자의 요청을 받으면, 가장 먼저 이 파일을 읽고 **'어떤 상세 문서가 필요한지'** 판단하여 요청하세요.
>
> last updated: 2026-02-08 19:16

## 1. Core Context (기반 지식)

> 프로젝트의 기술 스택, 구조, 핵심 원칙 등 변하지 않는 근간입니다.

| 문서                             | 설명                | ⚡ Trigger Intent (이럴 때 참조하세요) |
| :------------------------------- | :------------------ | :------------------------------------- |
| [Security Overview](./security/) | 보안 설정 관련 문서 | 인증/인가, 보안 정책 확인 시           |

## 2. Key Architecture Decisions (ADR)

> 우리가 '왜' 기술적인 결정을 내렸는지에 대한 합의 사항입니다. (status: Accepted 만 포함)

| ID  | 문서                                                                                             | ⚡ Trigger Intent (이럴 때 참조하세요)          |
| :-- | :----------------------------------------------------------------------------------------------- | :---------------------------------------------- |
| 001 | [Spring Security 도입](./adr/001-adopt-spring-security.md)                                       | 인증/인가 아키텍처 기반 기술 선정 배경 확인 시  |
| 002 | [REST API CSRF 비활성화](./adr/002-disable-csrf-for-rest-api.md)                                 | CSRF 보호 정책, REST API 보안 설정 시           |
| 003 | [Docker 컨테이너화](./adr/003-adopt-docker-containerization.md)                                  | 컨테이너 배포, Docker 설정 관련 작업 시         |
| 004 | [GitHub Actions 배포 파이프라인](./adr/004-establish-deployment-pipeline-with-github-actions.md) | CI/CD 파이프라인, 배포 자동화 설정 시           |
| 005 | [모니터링 전략](./adr/005-adopt-monitoring-strategy.md)                                          | 로깅, 메트릭, 모니터링 설정 시                  |
| 006 | [프로파일별 로깅 전략](./adr/006-adopt-profile-specific-logging-strategy.md)                     | 환경별 로깅 설정, 로그 레벨 조정 시             |
| 007 | [CORS 정책 설정](./adr/007-configure-cors-policy.md)                                             | CORS 에러 해결, 허용 도메인 추가 시             |
| 008 | [TraceId 포함 전역 예외 처리](./adr/008-adopt-global-exception-handling-with-traceid.md)         | 에러 응답 형식, 예외 처리 로직 수정 시          |
| 009 | [GPG 암호화 민감 설정 관리](./adr/009-manage-sensitive-config-with-gpg-encryption.md)            | 환경변수, 시크릿 관리, 설정 암호화 관련 작업 시 |
| 010 | [학습 중인 Scene 관리 정책](./adr/010-learning-scene-management.md)                              | 학습 데이터 관리, 인기/진척도 판단 정책 확인 시 |
| 011 | [배치 기반 Scene 통계 집계](./adr/011-batch-scene-statistics-management.md)                      | Scene 랭킹 집계 방식, 배치 처리 전략 확인 시    |

## 3. Feature Specifications (도메인 로직)

> 비즈니스 로직과 기능 명세입니다. 기능 구현 시 가장 많이 참조됩니다.

| 문서                                     | 설명                                 | ⚡ Trigger Intent (이럴 때 참조하세요)             |
| :--------------------------------------- | :----------------------------------- | :------------------------------------------------- |
| [Login](./specs/auth/login.md)           | 로그인 API 및 인증 로직              | 로그인 구현/수정, JWT 토큰 발급, 인증 실패 처리 시 |
| [Onboarding](./specs/user/onboarding.md) | 온보딩 API 및 사용자 프로필 업데이트 | 온보딩 프로세스 수정, 사용자 프로필 필드 변경 시   |
| [Learning Scenes](./specs/scene/learning_scenes.md) | 학습 중인 Scene 목록 조회 | 홈 화면 학습 목록 수정, 진척도/인기 여부 로직 확인 시 |
| [Scene Ranks](./specs/scene/scene_ranks.md) | Scene 랭킹 조회 (1~5위) | 인기 Scene 순위 API 수정, 집계 시점 정책 확인 시 |

