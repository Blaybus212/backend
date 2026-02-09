package com.blaybus.backend.loader;

import java.io.File;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.alignment.Alignment;
import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.scene.SceneConfigDto;
import com.blaybus.backend.repository.AlignmentRepository;
import com.blaybus.backend.repository.ComponentRepository;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test") // Mock 충돌 방지 및 컨텍스트 오염 방지를 위해 테스트 환경에서는 실행하지 않음
@RequiredArgsConstructor
public class SceneDataLoader implements CommandLineRunner {

	private final UserRepository userRepository;
	private final SceneInformationRepository sceneRepository;
	private final ComponentRepository componentRepository;
	private final AlignmentRepository alignmentRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		log.info("🚀 Starting Scene Data Loader...");

		// 1. 기본 User 존재 여부 확인
		User defaultUser = userRepository.findById(1L).orElseGet(() -> {
			log.info("Default User 생성 (ID: 1)");
			return userRepository.save(User.builder()
				.username("admin")
				.password("admin1234!") // 더미 패스워드
				.name("Administrator")
				.isMockUser(false)
				.onBoardingCompleted(true)
				// .email(), .role() 등은 User 엔티티에 없음
				.build());
		});

		// 2. Assets 디렉토리 스캔
		// 가정: src/main/resources/assets 경로를 프로덕션/개발 환경의 에셋 루트로 사용
		// 실제 JAR 배포 환경에서는 ResourceLoader를 사용해야 할 수 있으나,
		// 현재는 IDE 실행 또는 압축 해제된 상태를 가정하여 파일 시스템 경로("src/main/resources")를 사용함.
		// 패키징된 배포 환경이라면 classpath 로딩 방식으로 변경 필요.
		// 현재 태스크에서는 프로젝트 루트 기준 파일 시스템 경로를 사용.
		String assetsPath = "src/main/resources/assets";
		File assetsDir = new File(assetsPath);

		if (!assetsDir.exists() || !assetsDir.isDirectory()) {
			log.warn("⚠️ Assets directory not found: {}", assetsDir.getAbsolutePath());
			return;
		}

		File[] sceneDirs = assetsDir.listFiles(File::isDirectory);
		if (sceneDirs == null) {
			return;
		}

		for (File sceneDir : sceneDirs) {
			processSceneDirectory(defaultUser, sceneDir);
		}

		log.info("✅ Scene Data Loader finished.");
	}

	private void processSceneDirectory(User user, File sceneDir) {
		String sceneName = sceneDir.getName();
		File configFile = new File(sceneDir, "config/assembly_config.json");

		if (!configFile.exists()) {
			log.debug("Skipping {} (No config found)", sceneName);
			return;
		}

		try {
			log.info("Processing Scene: {}", sceneName);
			SceneConfigDto config = objectMapper.readValue(configFile, SceneConfigDto.class);

			// 1. Scene 생성 또는 업데이트
			SceneInformation scene = sceneRepository.findByEngTitle(sceneName)
				.orElseGet(() -> sceneRepository.save(SceneInformation.builder()
					.title(sceneName)
					.engTitle(sceneName)
					.category(SceneCategory.MANUFACTURING_ENGINEERING)
					.assetPath(sceneName) // 폴더명과 일치
					.description("자동 로드된 Scene")
					.participantsCount(0L) // 필수 필드 초기화
					.defaultAlignmentId(0L) // 필수 필드 (임시 0, 추후 업데이트 필요할 수 있음)
					.build()));

			// 2. 인스턴스(Nodes) 처리
			java.util.Map<String, String> assetsMap = config.getAssets();
			if (config.getInstances() != null) {
				for (SceneConfigDto.InstanceDto instance : config.getInstances()) {
					processInstance(user, scene, instance, assetsMap);
				}
			}
		} catch (Exception e) {
			log.error("Failed to process scene: {}", sceneName, e);
		}
	}

	private void processInstance(User user, SceneInformation scene, SceneConfigDto.InstanceDto instance,
		java.util.Map<String, String> assetsMap) {
		// Component 이름 해석
		// instance.assetId 는 assets 맵의 키
		String componentName = instance.getAssetId();

		// resolve assetPath using map
		// default fallback if not found in map (e.g. key + .gltf)
		String tempPath = componentName + ".gltf";
		if (assetsMap != null && assetsMap.containsKey(componentName)) {
			tempPath = assetsMap.get(componentName);
		}
		final String resolvedAssetPath = tempPath;

		com.blaybus.backend.domain.alignment.Component component = componentRepository.findByName(componentName)
			.orElseGet(() -> {
				// 메타데이터 매핑 (User Request 반영)
				String desc = String.format("%s의 %s 부품입니다", scene.getTitle(), componentName);
				String usage = "제조, 조립, 용접, 도장, 검사 작업";
				String texture = "알루미늄 합금, 탄소 섬유, 고강도 플라스틱";

				return componentRepository.save(com.blaybus.backend.domain.alignment.Component.builder()
					.name(componentName)
					.description(desc)
					.usage(usage)
					.texture(texture)
					.assetPath(resolvedAssetPath)
					.build());
			});

		// Alignment 생성 또는 업데이트
		String nodeName = instance.getName();
		String matrixJson;
		try {
			matrixJson = objectMapper.writeValueAsString(instance.getMatrix());
		} catch (Exception e) {
			log.error("Matrix error", e);
			matrixJson = "[]";
		}

		Optional<Alignment> existing = alignmentRepository.findByUserIdAndSceneIdAndNodeName(user.getId(),
			scene.getId(), nodeName);

		if (existing.isPresent()) {
			// 업데이트가 필요한가? 로더의 경우, 기본 상태로 리셋하거나 스킵할 수 있음.
			log.debug("  기존 Alignment 스킵: {}", nodeName);
		} else {
			alignmentRepository.save(Alignment.builder()
				.user(user)
				.scene(scene)
				.component(component)
				.nodeName(nodeName)
				.transformMatrix(matrixJson)
				.build());
			log.debug("  Created alignment: {}", nodeName);
		}
	}
}
