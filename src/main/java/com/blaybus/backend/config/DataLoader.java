package com.blaybus.backend.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.blaybus.backend.domain.alignment.Component;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.repository.ComponentRepository;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시점: Spring Container 초기화 -> 모든 Bean 생성 -> 서버 시작 완료 -> ApplicationRunner.run()
 * 목적: 초기 mock 데이터를 삽입하여 공모전 시연에 사용하기 위함
 */

@Slf4j
@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ComponentRepository componentRepository;
	private final SceneInformationRepository sceneRepository;
	private final ObjectMapper objectMapper;
	private final ResourcePatternResolver resourcePatternResolver;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("모든 Bean이 load된 이후에 초기 mock 데이터를 삽입합니다");

		// 초기 사용자 데이터 삽입 (이미 존재하면 스킵)
		createUserIfNotExists("admin", "admin1234!");

		// 초기 컴포넌트 데이터 삽입
		loadInitialComponents();

		log.info("삽입 완료!");
	}

	private void createUserIfNotExists(String username, String rawPassword) {
		if (userRepository.findByUsername(username).isEmpty()) {
			User user = User.builder()
				.username(username)
				.password(passwordEncoder.encode(rawPassword))
				.isMockUser(true) // Mock 사용자로 표시
				.build();
			userRepository.save(user);
			log.info("Created mock user: {}", username);
		} else {
			log.info("User already exists, skipping: {}", username);
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
				sceneRepository.findByEngTitle(sceneName).ifPresentOrElse(scene -> {
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

				Component component = Component
					.builder()
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
