package com.blaybus.backend.config;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.SceneStatistics;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.domain.user.ThemeColor;
import com.blaybus.backend.domain.user.UserGrass;
import com.blaybus.backend.repository.ComponentRepository;
import com.blaybus.backend.repository.QuizRepository;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.domain.quiz.Quiz;
import com.blaybus.backend.domain.quiz.QuizType;
import com.blaybus.backend.repository.SceneStatisticsRepository;
import com.blaybus.backend.repository.UserGrassRepository;
import com.blaybus.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 시점: Spring Container 초기화 -> 모든 Bean 생성 -> 서버 시작 완료 -> ApplicationRunner.run()
 * 목적: 초기 mock 데이터를 삽입하여 공모전 시연에 사용하기 위함
 */

@Slf4j
@Component
@Profile("!test")
public class DataLoader implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ComponentRepository componentRepository;
	private final ObjectMapper objectMapper;
	private final ResourcePatternResolver resourcePatternResolver;

	private final SceneInformationRepository sceneInformationRepository;
	private final SceneStatisticsRepository sceneStatisticsRepository;
	private final UserGrassRepository userGrassRepository;
	private final QuizRepository quizRepository;

	public DataLoader(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			ComponentRepository componentRepository,
			@Qualifier("objectMapper") ObjectMapper objectMapper,
			ResourcePatternResolver resourcePatternResolver,
			SceneInformationRepository sceneInformationRepository,
			SceneStatisticsRepository sceneStatisticsRepository,
			UserGrassRepository userGrassRepository,
			QuizRepository quizRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.componentRepository = componentRepository;
		this.objectMapper = objectMapper;
		this.resourcePatternResolver = resourcePatternResolver;
		this.sceneInformationRepository = sceneInformationRepository;
		this.quizRepository = quizRepository;
		this.sceneStatisticsRepository = sceneStatisticsRepository;
		this.userGrassRepository = userGrassRepository;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("모든 Bean이 load된 이후에 초기 mock 데이터를 삽입합니다");

		// 초기 사용자 데이터 삽입 (이미 존재하면 스킵)
		User admin = createUserIfNotExists("admin", "admin1234!");

		// 초기 씬 데이터 삽입
		loadInitialScenes();

		// 초기 컴포넌트 데이터 삽입
		loadInitialComponents();

		// 초기 퀴즈 데이터 삽입
		loadInitialQuizzes();

		// 기본 SceneStatistics 데이터 삽입 (시연용)
		loadInitialStatistics();

		// 초기 UserGrass (잔디) 데이터 삽입
		if (admin != null) {
			LocalDate today = LocalDate.now();
			createUserGrassIfNotExists(admin, today, 0, 0, 3); // 오늘: 15점, 5문제, streak 3
			createUserGrassIfNotExists(admin, today.minusDays(1), 10, 5, 2); // 어제: 10점, 5문제, streak 2
			createUserGrassIfNotExists(admin, today.minusDays(2), 5, 2, 1); // 그제: 5점, 2문제, streak 1
			createUserGrassIfNotExists(admin, today.minusDays(5), 25, 10, 5); // 5일전: 25점, 10문제, streak 5
			createUserGrassIfNotExists(admin, today.minusDays(10), 0, 0, 0); // 10일전: 0점, 0문제, streak 0
		}

		log.info("삽입 완료!");
	}

	private User createUserIfNotExists(String username, String rawPassword) {
		return userRepository.findByUsername(username).orElseGet(() -> {
			User user = User.builder()
					.username(username)
					.password(passwordEncoder.encode(rawPassword))
					.onBoardingCompleted(false)
					.isMockUser(true) // Mock 사용자로 표시
					.themeColor(ThemeColor.GREEN)
					.build();
			userRepository.save(user);
			log.info("Created mock user: {}", username);
			return user;
		});
	}

	private void createSceneStatisticsIfNotExists(SceneInformation scene, LocalDateTime aggregatedTime,
			Integer score, Integer rank, Integer difference) {
		if (!sceneStatisticsRepository.existsBySceneAndAggregatedTime(scene, aggregatedTime)) {
			SceneStatistics statistics = SceneStatistics.builder()
					.scene(scene)
					.aggregatedTime(aggregatedTime)
					.score(score)
					.rank(rank)
					.difference(difference)
					.build();
			sceneStatisticsRepository.save(statistics);
			log.info("Created mock scene statistics for scene: {} at {}", scene.getTitle(), aggregatedTime);
		}
	}

	private void createUserGrassIfNotExists(User user, LocalDate date, Integer score, Integer solvedCount,
			Integer streak) {
		if (userGrassRepository.findByUserAndDate(user, date).isEmpty()) {
			UserGrass grass = UserGrass.builder()
					.user(user)
					.date(date)
					.score(score)
					.solvedCount(solvedCount)
					.streak(streak)
					.build();
			userGrassRepository.save(grass);
			log.info("Created mock user-grass for user: {} at {}", user.getUsername(), date);
		}
	}

	private void loadInitialScenes() {
		try {
			Resource resource = resourcePatternResolver.getResource("classpath:data/initial_scene_data.json");
			if (!resource.exists()) {
				log.warn("Initial scene data file not found.");
				return;
			}
			JsonNode root = objectMapper.readTree(resource.getInputStream());
			if (!root.isArray()) {
				log.warn("Initial scene data is not a JSON Array.");
				return;
			}

			for (JsonNode node : root) {
				String engTitle = node.path("eng_title").asText();
				if (sceneInformationRepository.findByEngTitle(engTitle).isPresent()) {
					continue;
				}

				String categoryText = node.path("category").asText();
				SceneCategory category;
				try {
					// 먼저 영문 value로 시도
					category = SceneCategory.fromValue(categoryText);
				} catch (IllegalArgumentException e) {
					// 한글 displayName인 경우 처리
					category = java.util.Arrays.stream(SceneCategory.values())
							.filter(c -> c.getDisplayName().equals(categoryText))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("Unknown category: " + categoryText));
				}

				SceneInformation scene = SceneInformation.builder()
						.title(node.path("title").asText())
						.engTitle(engTitle)
						.assetPath(node.path("asset_path").asText())
						.category(category)
						.description(node.path("description").asText())
						.participantsCount(node.path("participants_count").asLong(0))
						.defaultAlignmentId(0L) // 기본값 설정
						.build();

				sceneInformationRepository.save(scene);
				log.info("Created scene: {}", engTitle);
			}

		} catch (IOException e) {
			log.error("Failed to load initial scene data", e);
		}
	}

	private void loadInitialComponents() {
		try {
			// 1. 사용자 제공 메타데이터 로드
			Resource resource = resourcePatternResolver.getResource("classpath:data/initial_component_data.json");
			if (!resource.exists()) {
				log.warn("Initial component data file not found.");
				return;
			}
			JsonNode metadataRoot = objectMapper.readTree(resource.getInputStream());

			// 2. 각 Scene 별로 처리
			metadataRoot.fields().forEachRemaining(sceneEntry -> {
				String sceneName = sceneEntry.getKey();
				JsonNode componentsNode = sceneEntry.getValue();

				// Scene 정보 조회 (Asset Path 확인용)
				sceneInformationRepository.findByEngTitle(sceneName).ifPresentOrElse(scene -> {
					processSceneComponents(scene, componentsNode);
				}, () -> log.warn("Scene not found for metadata: {}", sceneName));
			});

		} catch (IOException e) {
			log.error("Failed to load initial component data", e);
		}
	}

	private void processSceneComponents(SceneInformation scene, JsonNode componentsMetadata) {
		String assetPath = scene.getAssetPath();
		String configPath = "classpath:assets/" + assetPath + "/config/assembly_config.json";

		try {
			Resource configResource = resourcePatternResolver.getResource(configPath);
			if (!configResource.exists()) {
				log.warn("Assembly config not found for scene: {}", assetPath);
				return;
			}
			JsonNode configRoot = objectMapper.readTree(configResource.getInputStream());
			JsonNode instancesNode = configRoot.path("instances");
			JsonNode assetsNode = configRoot.path("assets");

			// Node Name -> Asset ID 매핑 생성
			Map<String, String> nodeToAssetId = new HashMap<>();
			if (instancesNode.isArray()) {
				for (JsonNode inst : instancesNode) {
					nodeToAssetId.put(inst.path("name").asText(), inst.path("assetId").asText());
				}
			}

			// Metadata 순회하며 Component 생성/업데이트
			componentsMetadata.fields().forEachRemaining(entry -> {
				String nodeName = entry.getKey(); // 예: Arm_gear1
				JsonNode meta = entry.getValue();

				// Node Name으로 Asset ID 찾기
				String assetId = nodeToAssetId.get(nodeName);
				if (assetId == null) {
					log.debug("No mapping found for node: {} in scene: {}", nodeName, scene.getTitle());
					return;
				}

				// 이미 존재하는지 확인 (Asset ID 기준)
				if (componentRepository.findByName(assetId).isPresent()) {
					return; // 이미 존재하면 스킵
				}

				// Asset Filename 찾기
				String filename = assetsNode.path(assetId).asText(assetId + ".gltf");

				var component = com.blaybus.backend.domain.alignment.Component.builder()
						.name(assetId)
						.description(meta.path("description").asText())
						.texture(meta.path("texture").asText())
						.usage(meta.path("usage").asText())
						.assetPath(filename)
						.build();

				componentRepository.save(component);
				log.info("Created component: {} (Asset: {})", assetId, filename);
			});

		} catch (IOException e) {
			log.error("Failed to process components for scene: {}", scene.getTitle(), e);
		}
	}

	private void loadInitialQuizzes() {
		try {
			Resource resource = resourcePatternResolver.getResource("classpath:data/initial_quiz_data.json");
			if (!resource.exists()) {
				log.warn("Initial quiz data file not found.");
				return;
			}
			JsonNode root = objectMapper.readTree(resource.getInputStream());
			if (!root.isArray()) {
				log.warn("Initial quiz data is not a JSON Array.");
				return;
			}

			for (JsonNode node : root) {
				String question = node.path("question").asText();
				if (quizRepository.existsByQuestion(question)) {
					continue;
				}

				String sceneTitle = node.path("scene_info_title").asText();
				sceneInformationRepository.findByTitle(sceneTitle).ifPresentOrElse(scene -> {
					try {
						Quiz quiz = Quiz.builder()
								.scene(scene)
								.targetPurpose(node.path("target_purpose").asText())
								.type(QuizType.valueOf(node.path("type").asText().toUpperCase()))
								.question(question)
								.answer(node.path("answer").asText())
								.build();
						quizRepository.save(quiz);
						log.info("Created quiz for scene: {}", sceneTitle);
					} catch (IllegalArgumentException e) {
						log.warn("Skipping quiz with invalid type for question [{}]: {}", question,
								node.path("type").asText());
					}
				}, () -> log.warn("Scene not found for quiz: {}", sceneTitle));
			}

		} catch (IOException e) {
			log.error("Failed to load initial quiz data", e);
		}
	}

	private void loadInitialStatistics() {
		try {
			Resource resource = resourcePatternResolver.getResource("classpath:data/initial_statistics_data.json");
			if (!resource.exists()) {
				log.warn("Initial statistics data file not found.");
				return;
			}
			JsonNode root = objectMapper.readTree(resource.getInputStream());
			if (!root.isArray()) {
				log.warn("Initial statistics data is not a JSON Array.");
				return;
			}

			for (JsonNode node : root) {
				String sceneTitle = node.path("scene_info_title").asText();
				int daysAgo = node.path("days_ago").asInt();

				LocalDateTime aggregatedTime = LocalDateTime.now().minusDays(daysAgo)
						.withHour(7).withMinute(0).withSecond(0).withNano(0);

				sceneInformationRepository.findByTitle(sceneTitle).ifPresentOrElse(scene -> {
					createSceneStatisticsIfNotExists(
							scene,
							aggregatedTime,
							node.path("score").asInt(),
							node.path("rank").asInt(),
							node.path("difference").asInt());
				}, () -> log.warn("Scene not found for statistics: {}", sceneTitle));
			}

		} catch (IOException e) {
			log.error("Failed to load initial statistics data", e);
		}
	}
}
