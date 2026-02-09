package com.blaybus.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.alignment.Alignment;
import com.blaybus.backend.domain.alignment.Component;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.scene.AssemblyRequestDto;
import com.blaybus.backend.dto.scene.ComponentStateDto;
import com.blaybus.backend.dto.scene.SceneAssemblyDto;
import com.blaybus.backend.dto.scene.SceneNodeDto;
import com.blaybus.backend.dto.scene.SceneSyncDto;
import com.blaybus.backend.repository.AlignmentRepository;
import com.blaybus.backend.repository.ComponentRepository;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.repository.UserSceneRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SceneAssemblyService {

	private final SceneInformationRepository sceneRepository;
	private final AlignmentRepository alignmentRepository;
	private final ComponentRepository componentRepository;
	private final UserRepository userRepository;
	private final UserSceneRepository userSceneRepository;
	private final ObjectMapper objectMapper;
	private final ResourcePatternResolver resourcePatternResolver;

	public void saveAssembly(Long userId, SceneAssemblyDto dto) {
		// 1. User 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

		// 2. Scene 조회
		String filePath = dto.getFile(); // 예: "Drone/Drone.gltf" 또는 "Drone"
		String sceneName = extractSceneName(filePath);
		SceneInformation scene = sceneRepository.findByEngTitle(sceneName)
			.or(() -> sceneRepository.findByTitle(sceneName))
			.orElseThrow(() -> new IllegalArgumentException("Scene not found for file: " + filePath));

		// 3. Node 처리
		for (SceneNodeDto node : dto.getNodes()) {
			processNode(user, scene, node);
		}
	}

	public void syncSceneState(Long userId, Long sceneId, SceneSyncDto dto) {
		// 1. LookAt 업데이트 (UserScene)
		if (dto.getLookAt() != null) {
			UserScene userScene = userSceneRepository.findByUserIdAndSceneId(userId, sceneId)
				.orElseGet(() -> {
					User user = userRepository.findById(userId)
						.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
					SceneInformation scene = sceneRepository.findById(sceneId)
						.orElseThrow(() -> new IllegalArgumentException("Scene not found: " + sceneId));

					return UserScene.builder()
						.user(user)
						.scene(scene)
						.lookAt("{}") // 필요 시 기본값 설정
						.build();
				});

			try {
				String lookAtJson = objectMapper.writeValueAsString(dto.getLookAt());
				// Entity 업데이트. Setter가 없으므로 Builder를 사용하여 복사본 생성 (ID 유지).
				// JPA에서는 Setters가 없는 불변 객체 패턴 시 "update" 메소드를 추가하거나,
				// 이처럼 Builder로 동일 ID 객체를 생성하여 save()를 호출하는 방식을 사용 가능.

				UserScene updatedUserScene = UserScene.builder()
					.id(userScene.getId())
					.user(userScene.getUser())
					.scene(userScene.getScene())
					.lookAt(lookAtJson)
					.note(userScene.getNote())
					.build();

				userSceneRepository.save(updatedUserScene);

			} catch (JsonProcessingException e) {
				log.error("LookAt 직렬화 실패", e);
				throw new RuntimeException("LookAt serialization failed", e);
			}
		}

		// 2. Components 업데이트 (Alignment)
		if (dto.getComponents() != null) {
			for (ComponentStateDto compState : dto.getComponents()) {
				updateComponentState(userId, sceneId, compState);
			}
		}
	}

	private void updateComponentState(Long userId, Long sceneId, ComponentStateDto compState) {
		String nodeName = compState.getNodeName();
		alignmentRepository.findByUserIdAndSceneIdAndNodeName(userId, sceneId, nodeName)
			.ifPresent(alignment -> {
				try {
					String matrixJson = objectMapper.writeValueAsString(compState.getMatrix());

					// UserScene과 유사하게 ID를 사용하여 Builder로 업데이트
					Alignment updatedAlignment = Alignment.builder()
						.id(alignment.getId())
						.user(alignment.getUser())
						.scene(alignment.getScene())
						.component(alignment.getComponent())
						.nodeName(alignment.getNodeName())
						.transformMatrix(matrixJson)
						.build();

					alignmentRepository.save(updatedAlignment);

				} catch (JsonProcessingException e) {
					log.error("Sync를 위한 Matrix 직렬화 실패: {}", nodeName, e);
				}
			});
		// 찾지 못한 경우, "sync"는 일반적으로 기존 상태 업데이트를 의미하므로 무시함.
		// Sync 시 새로운 인스턴스를 생성해야 한다면 추가 정보(Component 조회 등)가 필요함.
	}

	// ... (existing methods)

	public byte[] exportAssembledGltf(Long userId, Long sceneId) {
		// 1. 데이터 조회
		List<Alignment> alignments = alignmentRepository.findByUserIdAndSceneId(userId, sceneId);
		SceneInformation scene = sceneRepository.findById(sceneId)
			.orElseThrow(() -> new IllegalArgumentException("Scene not found"));

		if (alignments.isEmpty()) {
			throw new RuntimeException("No alignments found for this scene.");
		}

		// 2. Node.js용 JSON 준비
		// 2-1. Assets 맵 빌드
		Map<String, String> assetsMap = alignments.stream()
			.map(a -> a.getComponent().getName())
			.distinct()
			.collect(Collectors.toMap(
				name -> name,
				name -> name + ".gltf" // 현재는 단순 매핑 사용
			));

		// 2-2. Instances 리스트 빌드
		List<AssemblyRequestDto.AssemblyNodeDto> instanceDtos = alignments.stream().map(align -> {
			Component comp = align.getComponent();
			Map<String, Object> extras = new HashMap<>();
			extras.put("dbId", comp.getId());
			extras.put("description", comp.getDescription());
			extras.put("texture", comp.getTexture());
			// 필요 시 다른 메타데이터 추가

			List<Double> matrix;
			try {
				// @formatter:off
				matrix = objectMapper.readValue(align.getTransformMatrix(), new TypeReference<List<Double>>() { });
				// @formatter:on
			} catch (JsonProcessingException e) {
				log.error("Failed to parse matrix for align {}", align.getId());
				matrix = List.of(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
			}

			return AssemblyRequestDto.AssemblyNodeDto.builder()
				.name(align.getNodeName())
				.matrix(matrix)
				.assetId(comp.getName()) // Maps to key in assetsMap
				.extras(extras)
				.build();
		}).collect(Collectors.toList());

		AssemblyRequestDto requestDto = AssemblyRequestDto.builder()
			.instances(instanceDtos)
			.assets(assetsMap)
			.build();

		// 3. 임시 파일 및 스크립트 실행
		try {
			// Assets을 임시 디렉토리로 복사 (classpath에서)
			// Node.js 스크립트는 파일 시스템 경로가 필요하므로, JAR 내부 리소스를 임시 폴더로 추출해야 함.
			String assetPath = scene.getAssetPath();
			org.springframework.core.io.Resource[] assetResources = resourcePatternResolver
				.getResources("classpath*:assets/" + assetPath + "/**");

			if (assetResources.length == 0) {
				throw new RuntimeException("No assets found for scene: " + assetPath);
			}

			File tempAssetsDir = Files.createTempDirectory("assets_" + sceneId + "_").toFile();
			tempAssetsDir.deleteOnExit(); // JVM 종료 시 삭제 예약 (주의: 내용물이 있으면 삭제 안 될 수 있음)

			for (org.springframework.core.io.Resource res : assetResources) {
				String filename = res.getFilename();
				if (filename == null) {
					continue;
				}

				// classpath 리소스 구조를 유지하며 복사할 수 있는지 확인 필요.
				// classpath*:assets/Drone/Drone.gltf -> temp/Drone.gltf
				// 여기서는 resource.getURI() 등을 파싱하거나, 단순 플랫하게 복사.
				// 일단 플랫하게 복사한다고 가정 (하위 디렉토리 구조 복잡성 회피).
				// 만약 하위 폴더가 중요하다면 계층 구조 파싱 필요.
				// 현재 에셋 구조는 assets/{SceneName}/*.gltf 로 가정.

				File destFile = new File(tempAssetsDir, filename);
				if (!res.getURI().toString().endsWith("/")) { // 디렉토리가 아닌 경우만 복사
					try (java.io.InputStream is = res.getInputStream()) {
						Files.copy(is, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}

			File inputJson = File.createTempFile("assembly_req_" + userId + "_", ".json");
			objectMapper.writeValue(inputJson, requestDto);

			return executeNodeAssembly(inputJson, tempAssetsDir.getAbsolutePath());

		} catch (IOException e) {
			throw new RuntimeException("Failed to load assets or create temp file for export", e);
		}
	}

	private byte[] executeNodeAssembly(File inputJson, String assetsDir) {
		File workingDir = null;
		try {
			// Node.js 스크립트도 classpath에서 추출 필요
			org.springframework.core.io.Resource scriptResource = resourcePatternResolver
				.getResource("classpath:scripts/assemble_pro.js");
			if (!scriptResource.exists()) {
				throw new RuntimeException("Script not found: classpath:scripts/assemble_pro.js");
			}

			File tempScript = File.createTempFile("assemble_pro_", ".js");
			try (java.io.InputStream is = scriptResource.getInputStream()) {
				Files.copy(is, tempScript.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}

			File outputFile = File.createTempFile("assembled_output_", ".gltf");

			// assetsDir 내부에 .gltf 파일들이 있어야 함.

			ProcessBuilder pb = new ProcessBuilder(
				"node",
				tempScript.getAbsolutePath(),
				inputJson.getAbsolutePath(),
				assetsDir,
				outputFile.getAbsolutePath());

			pb.redirectErrorStream(true);

			// 작업 디렉토리 설정 (선택 사항)
			// pb.directory(new File("."));

			Process process = pb.start();

			String output = new String(process.getInputStream().readAllBytes());
			int exitCode = process.waitFor();

			// Cleanup script
			tempScript.delete();

			if (exitCode != 0) {
				log.error("Node.js assembly failed. Exit code: {}\nOutput: {}", exitCode, output);
				inputJson.delete();
				outputFile.delete();
				throw new RuntimeException("GLTF assembly process failed.");
			}

			byte[] resultBytes = Files.readAllBytes(outputFile.toPath());

			// Cleanup
			inputJson.delete();
			outputFile.delete();

			return resultBytes;
		} catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Failed to execute Node.js assembly", e);
		}
	}

	public byte[] getViewerZip(Long userId, Long sceneId, String target) {
		SceneInformation scene = sceneRepository.findById(sceneId)
			.orElseThrow(() -> new IllegalArgumentException("Scene not found"));

		Map<String, byte[]> files = new HashMap<>();
		String manifestJson = "{}";
		Map<String, String> manifestMap = new HashMap<>();

		boolean includeDefault = "both".equalsIgnoreCase(target) || "default".equalsIgnoreCase(target);
		boolean includeCustom = "both".equalsIgnoreCase(target) || "custom".equalsIgnoreCase(target);

		if (includeDefault) {
			try {
				byte[] defaultGltf = generateDefaultGltf(scene);
				files.put("default.gltf", defaultGltf);
				manifestMap.put("default", "default.gltf");
			} catch (Exception e) {
				log.error("Failed to generate default GLTF", e);
				// Decide if we should fail hard or just skip.
				// For now, let's allow partial success or fail hard depending on requirements.
				// Assuming "Viewer" needs requested files, fail hard is safer to detect configs.
				throw new RuntimeException("Failed to generate default GLTF", e);
			}
		}

		if (includeCustom) {
			try {
				byte[] customGltf = exportAssembledGltf(userId, sceneId);
				files.put("custom.gltf", customGltf);
				manifestMap.put("custom", "custom.gltf");
			} catch (Exception e) {
				log.error("Failed to generate custom GLTF", e);
				// If no custom state exists, maybe we shouldn't fail if default worked?
				// But exportAssembledGltf throws if no alignments.
				// Let's handle "No alignments" gracefully if needed, but for now rethrow.
				throw e;
			}
		}

		try {
			manifestJson = objectMapper.writeValueAsString(manifestMap);
			files.put("manifest.json", manifestJson.getBytes());
			return createZip(files);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create ZIP", e);
		}
	}

	private byte[] generateDefaultGltf(SceneInformation scene) {
		String assetPath = scene.getAssetPath();
		String configPath = "classpath:assets/" + assetPath + "/config/assembly_config.json";

		try {
			org.springframework.core.io.Resource configResource = resourcePatternResolver.getResource(configPath);
			if (!configResource.exists()) {
				throw new RuntimeException("Default assembly config not found at: " + configPath);
			}

			File tempConfig = File.createTempFile("default_config_", ".json");
			try (java.io.InputStream is = configResource.getInputStream()) {
				Files.copy(is, tempConfig.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}

			// Assets 임시 디렉토리 준비
			org.springframework.core.io.Resource[] assetResources = resourcePatternResolver
				.getResources("classpath*:assets/" + assetPath + "/**");
			File tempAssetsDir = Files.createTempDirectory("assets_def_" + scene.getId() + "_").toFile();
			tempAssetsDir.deleteOnExit();

			for (org.springframework.core.io.Resource res : assetResources) {
				String filename = res.getFilename();
				if (filename == null) {
					continue;
				}
				if (!res.getURI().toString().endsWith("/")) {
					File destFile = new File(tempAssetsDir, filename);
					try (java.io.InputStream is = res.getInputStream()) {
						Files.copy(is, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}

			return executeNodeAssembly(tempConfig, tempAssetsDir.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException("Failed to prepare default config temp file", e);
		}
	}

	private byte[] createZip(Map<String, byte[]> files) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			for (Map.Entry<String, byte[]> entry : files.entrySet()) {
				ZipEntry zipEntry = new ZipEntry(entry.getKey());
				zos.putNextEntry(zipEntry);
				zos.write(entry.getValue());
				zos.closeEntry();
			}
		}
		return baos.toByteArray();
	}

	private void processNode(User user, SceneInformation scene, SceneNodeDto node) {
		String nodeName = node.getName(); // 예: "Arm_gear1"
		String componentName = deriveComponentName(nodeName); // 예: "Arm gear"

		// Component 조회 또는 생성
		Component component = componentRepository.findByName(componentName)
			.orElseGet(() -> {
				log.info("Creating new component: {}", componentName);
				return componentRepository.save(Component.builder()
					.name(componentName)
					.description("Auto-generated from assembly")
					.build());
			});

		// Matrix 직렬화
		String matrixJson;
		try {
			matrixJson = objectMapper.writeValueAsString(node.getMatrix());
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize matrix for node: {}", nodeName, e);
			throw new RuntimeException("Matrix serialization failed", e);
		}

		// 기존 Alignment 조회 또는 생성
		Alignment alignment = alignmentRepository
			.findByUserIdAndSceneIdAndNodeName(user.getId(), scene.getId(), nodeName)
			.orElse(Alignment.builder()
				.user(user)
				.scene(scene)
				.component(component)
				.nodeName(nodeName)
				.build());

		// Matrix 업데이트 (필요 시 정의도)
		// 신규 생성이면 Builder로 업데이트하지만, 기존이면 값을 설정해야 함.
		// Alignment 엔티티는 Setters가 없음(Lombok @Value 또는 Getter/Builder).
		// 따라서 다시 저장(save)해야 함. repo.save()는 ID가 있으면 업데이트함.

		// 이미 조회했다면 ID가 있음.
		// 업데이트를 위해 동일 ID를 가진 새 인스턴스를 생성해야 함.
		Alignment toSave = Alignment.builder()
			.id(alignment.getId()) // 신규면 null, 조회됐으면 기존 ID
			.user(user)
			.scene(scene)
			.component(component)
			.nodeName(nodeName)
			.transformMatrix(matrixJson)
			.build();

		alignmentRepository.save(toSave);
	}

	private String extractSceneName(String filePath) {
		// "Drone/Drone.gltf" -> "Drone"
		// "Car.gltf" -> "Car"
		if (filePath == null) {
			return "";
		}
		String name = filePath;
		int lastSlash = name.lastIndexOf('/');
		if (lastSlash >= 0) {
			name = name.substring(lastSlash + 1);
		}
		int dot = name.lastIndexOf('.');
		if (dot >= 0) {
			name = name.substring(0, dot);
		}
		return name;
	}

	private String deriveComponentName(String nodeName) {
		// "Arm_gear1" -> "Arm gear"
		if (nodeName == null) {
			return "Unknown";
		}
		// Remove trailing numbers
		String name = nodeName.replaceAll("\\d+$", "");
		// Replace underscores with spaces
		name = name.replace('_', ' ');
		return name.trim();
	}
}
