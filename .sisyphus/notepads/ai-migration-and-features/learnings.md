# Learnings

## Task 2: Quiz Grading Endpoint (2026-02-08)
- OpenAI RestClient bean은 `@Qualifier("openAiRestClient")`로 주입 (OpenAiService 패턴 참조)
- base-url이 `https://api.openai.com/v1`이므로 embedding endpoint는 `/embeddings`
- QuizUserProgress는 `user` + `scene` (SceneInformation) 조합으로 unique — repository 메서드명은 `findByUserIdAndSceneId`
- SceneRanksQuiz.scene 필드는 `SceneInformation` 타입 ManyToOne (숫자 ID가 아님)
- QuizUserProgress에 Setter 없음 → Builder로 새 인스턴스 만들어 save (id 유지)
- ConversationController 패턴: `findUser(username)` private 메서드로 User 조회
- Naver checkstyle + spotlessApply 통과 필수

## Task 3: Token Monitoring with Micrometer (2026-02-08)
- OpenAiDto.ResponsesResponse에 Usage record 추가 (input_tokens, output_tokens, total_tokens)
- OpenAiService와 EmbeddingService에 MeterRegistry 주입 (Spring Bean으로 자동 제공)
- Counter.builder() 패턴으로 메트릭 등록: `openai.tokens.input`, `openai.tokens.output`, `openai.tokens.embedding.input`
- null safety 체크 필수: `if (response.usage() != null)` 
- Import 순서 중요: org.springframework imports 먼저, 그 다음 com.blaybus, 그 다음 io.micrometer, 마지막 lombok
- 메트릭은 aggregate counter만 사용 (유저별/대화별 분리 추적 안 함)
- `/actuator/prometheus`에서 자동으로 노출됨 (application.yml에 이미 설정됨)

---

## Work Session Summary (Orchestrator)

**All Tasks Completed**: 2026-02-08
**Total Commits**: 4 on ai-feat branch
**Compilation**: BUILD SUCCESSFUL

### Final Checklist
- [x] Task 0: AI 코드 엔티티 마이그레이션
- [x] Task 1: User 개인화 프롬프트
- [x] Task 2: 퀴즈 임베딩 채점  
- [x] Task 3: 토큰 사용량 모니터링

### Commits
1. `46a4d92` - refactor(ai): 새 엔티티 패키지 구조에 맞춰 AI 코드 마이그레이션
2. `d63053f` - feat(prompt): 페르소나 기반 시스템 프롬프트 개인화 추가
3. `9436a5b` - feat(quiz): 퀴즈 채점 엔드포인트 및 임베딩 기반 스코어링 추가
4. `62b2f36` - feat(monitoring): OpenAI 토큰 사용량 Micrometer 메트릭 추가

### Key Achievements
- 4 OLD entity files deleted, imports migrated to new package structure
- 4 persona-specific prompts implemented with education/specialization context
- Quiz grading endpoint with SELECT (exact match) and INPUT (embedding similarity >= 0.8) modes
- Micrometer token counters exposed on /actuator/prometheus

### Note on Checkstyle
- 50 checkstyle warnings remain but are from pre-existing feat/23-add-entity merged entity files
- Our AI code (services, controllers, repositories, DTOs, config) passes checkstyle completely
- Fixed: SceneInformationRepository.java and QuizRepository.java whitespace issues

### Final Status: COMPLETE
**Date**: 2026-02-08
**Branch**: ai-feat
**Commits**: 5
1. `46a4d92` - refactor(ai): 새 엔티티 패키지 구조에 맞춰 AI 코드 마이그레이션
2. `d63053f` - feat(prompt): 페르소나 기반 시스템 프롬프트 개인화 추가
3. `9436a5b` - feat(quiz): 퀴즈 채점 엔드포인트 및 임베딩 기반 스코어링 추가
4. `62b2f36` - feat(monitoring): OpenAI 토큰 사용량 Micrometer 메트릭 추가
5. `c7e529b` - style(repository): fix checkstyle whitespace violations

### Verification Status
- ✅ `./gradlew compileJava` - BUILD SUCCESSFUL
- ✅ `./gradlew build -x test -x checkstyleTest` - BUILD SUCCESSFUL
- ✅ AI code checkstyle - PASSED (no violations in service/controller/repository/dto/config)
- ⚠️ `./gradlew checkstyleMain` - FAILS (50 warnings in pre-existing entity files from feat/23-add-entity merge)
- ⏳ Runtime tests - PENDING (requires running server + OPENAI_API_KEY + DB data)

### Blockers for Full Verification
1. **Runtime tests require**: Running server, OPENAI_API_KEY environment variable, database with test data
2. **CheckstyleMain fails**: Pre-existing violations in feat/23-add-entity merged entity files (not our code)

### Ready for
- [x] Code review
- [x] Push to origin: `git push origin ai-feat`
- [x] Create PR to develop
- [ ] Deploy to test environment for runtime verification
