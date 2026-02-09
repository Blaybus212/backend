package com.blaybus.backend.verification;

import com.blaybus.backend.domain.alignment.Alignment;
import com.blaybus.backend.domain.alignment.Component;
import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.scene.SceneConfigDto;
import com.blaybus.backend.repository.AlignmentRepository;
import com.blaybus.backend.repository.ComponentRepository;
import com.blaybus.backend.repository.QuizRepository;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.SceneStatisticsRepository;
import com.blaybus.backend.repository.UserGrassRepository;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.service.SceneAssemblyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Disabled("Manual verification only - requires Node.js environment")
@SpringBootTest
@ActiveProfiles("test")
public class ManualVerificationSyncTest {

	@Autowired
	private SceneAssemblyService sceneAssemblyService;

	@Autowired
	private SceneInformationRepository sceneRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AlignmentRepository alignmentRepository;

	@Autowired
	private ComponentRepository componentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ResourcePatternResolver resourcePatternResolver;

	@Autowired
	private SceneStatisticsRepository sceneStatisticsRepository;

	@Autowired
	private UserGrassRepository userGrassRepository;

	@Autowired
	private QuizRepository quizRepository;

	@Test
	@DisplayName("Generate extreme sync verification files")
	@Transactional // Ensure test data rollback, but files persist
	void generateExtremeSyncFiles() throws Exception {
		// 1. Setup Data using DataLoader logic or direct creation
		// We'll create specialized data for this test to be precise
		User user = userRepository.save(User.builder()
			.username("extreme_tester")
			.password(passwordEncoder.encode("pass"))
			.name("Extreme Tester")
			.isMockUser(false)
			.onBoardingCompleted(true)
			.build());

		// Ensure "Drone" scene exists (or use existing one)
		List<SceneInformation> scenes = sceneRepository.findAll();
		SceneInformation droneScene = scenes.stream()
			.filter(s -> "Drone".equals(s.getEngTitle()))
			.findFirst()
			.orElseGet(() -> {
				// Fallback: create if not exists
				return sceneRepository.save(SceneInformation.builder()
					.title("드론 조립")
					.engTitle("Drone")
					.category(SceneCategory.MANUFACTURING_ENGINEERING)
					.assetPath("Drone")
					.description("Drone Test Scene")
					.participantsCount(0L)
					.defaultAlignmentId(0L)
					.build());
			});

		// 2. Load Config to find "Arm_gear1"
		File configFile = new File("src/main/resources/assets/Drone/config/assembly_config.json");
		SceneConfigDto config = objectMapper.readValue(configFile, SceneConfigDto.class);

		// Find target instance
		String targetNodeName = "Arm_gear1";
		String targetAssetId = config.getInstances().stream()
			.filter(i -> targetNodeName.equals(i.getName()))
			.map(i -> i.getAssetId())
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Node " + targetNodeName + " not found in Drone config"));

		// Ensure component exists with CORRECT file name
		// "arm_gear" maps to "Arm gear.gltf" in reality
		String finalAssetPath = "arm_gear".equals(targetAssetId) ? "Arm gear.gltf" : targetAssetId + ".gltf";

		Component component = componentRepository.findByName(targetAssetId)
			.orElseGet(() -> componentRepository.save(Component.builder()
				.name(targetAssetId)
				.description("Test Component")
				.assetPath(finalAssetPath)
				.build()));

		// 3. Create Alignment with EXTREME Matrix
		// Matrix: Scale(100), Position(1000, 1000, 1000)
		// Format: Column-major 4x4
		// Identity:
		// 1 0 0 0
		// 0 1 0 0
		// 0 0 1 0
		// 0 0 0 1

		// Scaled(100):
		// 100 0 0 0
		// 0 100 0 0
		// 0 0 100 0
		// 0 0 0 1

		// Translated(1000, 1000, 1000): stored in last column (m14, m24, m34), which is index 12, 13, 14 in flat array
		// But depending on library convention (column vs row major). glTF uses Column Major.
		// So indices 12, 13, 14 are translation X, Y, Z.

		// 3. Create Alignment with MODERATE "Visible" Matrix
		// Scale(2.0), Position(5.0, 0.0, 0.0) -> Should be clearly visible near origin
		List<Double> moderateMatrix = Arrays.asList(
			2.0, 0.0, 0.0, 0.0,
			0.0, 2.0, 0.0, 0.0,
			0.0, 0.0, 2.0, 0.0,
			5.0, 0.0, 0.0, 1.0);
		String matrixJson = objectMapper.writeValueAsString(moderateMatrix);

		alignmentRepository.save(Alignment.builder()
			.user(user)
			.scene(droneScene)
			.component(component)
			.nodeName(targetNodeName)
			.transformMatrix(matrixJson)
			.build());

		System.out.println("✅ Created Alignment for " + targetNodeName + " with MODERATE matrix: " + matrixJson);

		// 4. Generate Viewer ZIP
		System.out.println("⏳ Generating Viewer ZIP...");
		byte[] zipBytes = sceneAssemblyService.getViewerZip(user.getId(), droneScene.getId(), "both");

		// 5. Save and Unzip
		Path outputDir = Paths.get("verification_exports/extreme_sync_test");
		Files.createDirectories(outputDir);

		// Clean directory
		File dir = outputDir.toFile();
		for (File file : dir.listFiles()) {
			if (!file.isDirectory())
				file.delete();
		}

		System.out.println("💾 Saving output to: " + outputDir.toAbsolutePath());

		try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(zipBytes);
			ZipInputStream zis = new ZipInputStream(bais)) {

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File file = outputDir.resolve(entry.getName()).toFile();
				try (FileOutputStream fos = new FileOutputStream(file)) {
					fos.write(zis.readAllBytes());
				}
				System.out.println("   - Extracted: " + entry.getName());
			}
		}

		System.out.println("🎉 Done! Check 'custom.gltf' in " + outputDir.toString());
		System.out.println("   You should see '" + targetNodeName + "' extremely large and far away.");
	}
}
