package com.blaybus.backend.verification;

import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.service.SceneAssemblyService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Disabled("Manual verification only")
@SpringBootTest
@ActiveProfiles("test")
public class ManualVerificationExportTest {

	@Autowired
	private SceneAssemblyService sceneAssemblyService;

	@Autowired
	private SceneInformationRepository sceneRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private com.blaybus.backend.config.DataLoader dataLoader;

	@Test
	@DisplayName("Export ZIPs for all scenes for manual verification")
	void exportAllScenes() throws Exception {
		// Ensure data exists
		dataLoader.run(null);

		// Get admin user
		User admin = userRepository.findByUsername("admin")
			.orElseThrow(() -> new RuntimeException("Admin user not found"));

		List<SceneInformation> scenes = sceneRepository.findAll();
		if (scenes.isEmpty()) {
			throw new RuntimeException("No scenes found to export");
		}

		// Create output directory
		Path outputDir = Paths.get("verification_exports");
		Files.createDirectories(outputDir);
		System.out.println("Exporting ZIPs to: " + outputDir.toAbsolutePath());

		for (SceneInformation scene : scenes) {
			try {
				System.out.println("Exporting scene: " + scene.getEngTitle());
				byte[] zipBytes = sceneAssemblyService.getViewerZip(admin.getId(), scene.getId(), "both");

				String filename = scene.getEngTitle().replaceAll(" ", "_") + "_viewer.zip";
				File file = outputDir.resolve(filename).toFile();

				try (FileOutputStream fos = new FileOutputStream(file)) {
					fos.write(zipBytes);
				}
				System.out.println("Saved: " + file.getAbsolutePath());
			} catch (Exception e) {
				System.err.println("Failed to export scene: " + scene.getEngTitle());
				e.printStackTrace();
			}
		}
	}
}
