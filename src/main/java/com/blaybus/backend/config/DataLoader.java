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
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.domain.user.UserGrass;
import com.blaybus.backend.repository.ComponentRepository;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.SceneStatisticsRepository;
import com.blaybus.backend.repository.UserGrassRepository;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.repository.UserSceneRepository;
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
	private final UserSceneRepository userSceneRepository;
	private final SceneStatisticsRepository sceneStatisticsRepository;
	private final UserGrassRepository userGrassRepository;

	public DataLoader(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		ComponentRepository componentRepository,
		@Qualifier("objectMapper")
		ObjectMapper objectMapper,
		ResourcePatternResolver resourcePatternResolver,
		SceneInformationRepository sceneInformationRepository,
		UserSceneRepository userSceneRepository,
		SceneStatisticsRepository sceneStatisticsRepository,
		UserGrassRepository userGrassRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.componentRepository = componentRepository;
		this.objectMapper = objectMapper;
		this.resourcePatternResolver = resourcePatternResolver;
		this.sceneInformationRepository = sceneInformationRepository;
		this.userSceneRepository = userSceneRepository;
		this.sceneStatisticsRepository = sceneStatisticsRepository;
		this.userGrassRepository = userGrassRepository;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("모든 Bean이 load된 이후에 초기 mock 데이터를 삽입합니다");

		// 초기 사용자 데이터 삽입 (이미 존재하면 스킵)
		User admin = createUserIfNotExists("admin", "admin1234!");

		// 초기 Scene 정보 데이터 삽입 (이미 존재하면 스킵)
		SceneInformation scene1 = createSceneInformationIfNotExists(1L, "로봇 팔", "Robot Arm", SceneCategory.ROBOTICS,
			45L, "심장의 펌프 작용과 혈류 역학을 3D로 학습합니다.", "https://example.com/thumb1.jpg");
		SceneInformation scene2 = createSceneInformationIfNotExists(2L, "자동차 엔진 4행정", "4-Stroke Engine Cycle",
			SceneCategory.AEROSPACE_ENGINEERING,
			32L, "내연기관의 4행정 사이클을 분해 조립하며 학습합니다.", "https://example.com/thumb2.jpg");
		SceneInformation scene3 = createSceneInformationIfNotExists(3L, "반도체 클린룸 공정", "Semiconductor Fab Process",
			SceneCategory.ROBOTICS, 12L, "웨이퍼 가공부터 패키징까지의 클린룸 공정을 시뮬레이션합니다.", "https://example.com/thumb3.jpg");

		// 초기 UserScene (최근 학습 데이터) 삽입
		if (admin != null) {
			createUserSceneIfNotExists(admin, scene1, "{\"x\": 10, \"y\": 20, \"z\": 30}",
				"심장 판막의 움직임이 인상적임. 다시 볼 필요 있음.", LocalDateTime.now().minusHours(1));
			createUserSceneIfNotExists(admin, scene2, "{\"x\": 0, \"y\": 0, \"z\": 100}", "흡기 행정에서 밸브 타이밍 확인 완료.",
				LocalDateTime.now().minusHours(2));
			createUserSceneIfNotExists(admin, scene3, "{\"x\": 50, \"y\": 50, \"z\": 50}", "노광 공정 파트가 복잡함. 추가 학습 예정.",
				LocalDateTime.now().minusHours(3));
		}

		// 기본 SceneStatistics 데이터 삽입 (시연용)
		// 어제 07:00 기준 랭킹 데이터
		LocalDateTime yesterday7am = LocalDateTime.now().minusDays(1).withHour(7).withMinute(0).withSecond(0)
			.withNano(0);
		createSceneStatisticsIfNotExists(scene1, yesterday7am, 450, 1, 2);
		createSceneStatisticsIfNotExists(scene2, yesterday7am, 320, 2, -1);
		createSceneStatisticsIfNotExists(scene3, yesterday7am, 180, 3, 1);

		// 그제 07:00 기준 랭킹 데이터 (비교용)
		LocalDateTime dayBeforeYesterday7am = LocalDateTime.now().minusDays(2).withHour(7).withMinute(0).withSecond(0)
			.withNano(0);
		createSceneStatisticsIfNotExists(scene1, dayBeforeYesterday7am, 300, 3, 0);
		createSceneStatisticsIfNotExists(scene2, dayBeforeYesterday7am, 350, 1, 0);
		createSceneStatisticsIfNotExists(scene3, dayBeforeYesterday7am, 200, 2, 0);

		// 초기 UserGrass (잔디) 데이터 삽입
		if (admin != null) {
			LocalDate today = LocalDate.now();
			createUserGrassIfNotExists(admin, today, 15, 5, 3); // 오늘: 15점, 5문제, streak 3
			createUserGrassIfNotExists(admin, today.minusDays(1), 10, 5, 2); // 어제: 10점, 5문제, streak 2
			createUserGrassIfNotExists(admin, today.minusDays(2), 5, 2, 1); // 그제: 5점, 2문제, streak 1
			createUserGrassIfNotExists(admin, today.minusDays(5), 25, 10, 5); // 5일전: 25점, 10문제, streak 5
			createUserGrassIfNotExists(admin, today.minusDays(10), 0, 0, 0); // 10일전: 0점, 0문제, streak 0
		}

		// 초기 씬 데이터 삽입
		loadInitialScenes();

		// 초기 컴포넌트 데이터 삽입
		loadInitialComponents();

		log.info("삽입 완료!");
	}

	private User createUserIfNotExists(String username, String rawPassword) {
		return userRepository.findByUsername(username).orElseGet(() -> {
			User user = User.builder()
				.username(username)
				.password(passwordEncoder.encode(rawPassword))
				.onBoardingCompleted(false)
				.isMockUser(true) // Mock 사용자로 표시
				.build();
			userRepository.save(user);
			log.info("Created mock user: {}", username);
			return user;
		});
	}

	private SceneInformation createSceneInformationIfNotExists(Long defaultAlignmentId, String title, String engTitle,
		SceneCategory category, Long participantsCount, String description, String thumbnailUrl) {
		return sceneInformationRepository.findByTitle(title)
			.orElseGet(() -> {
				SceneInformation scene = SceneInformation.builder()
					.defaultAlignmentId(defaultAlignmentId)
					.title(title)
					.engTitle(engTitle)
					.category(category)
					.participantsCount(participantsCount)
					.description(description)
					.thumbnailUrl(thumbnailUrl)
					.build();
				sceneInformationRepository.save(scene);
				log.info("Created mock scene: {}", title);
				return scene;
			});
	}

	private void createUserSceneIfNotExists(User user, SceneInformation scene, String lookAt, String note,
		LocalDateTime lastAccessedAt) {
		if (!userSceneRepository.existsByUser_IdAndScene_Id(user.getId(), scene.getId())) {
			UserScene userScene = UserScene.builder()
				.user(user)
				.scene(scene)
				.lookAt(lookAt)
				.note(note)
				.lastAccessedAt(lastAccessedAt)
				.build();
			userSceneRepository.save(userScene);
			log.info("Created mock user-scene mapping for user: {} and scene: {}", user.getUsername(),
				scene.getTitle());
		}
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
}
