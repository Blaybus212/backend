package com.blaybus.backend.config;

import java.time.LocalDateTime;

import com.blaybus.backend.domain.scene.SceneCategory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.repository.UserSceneRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시점: Spring Container 초기화 -> 모든 Bean 생성 -> 서버 시작 완료 -> ApplicationRunner.run()
 * 목적: 초기 mock 데이터를 삽입하여 공모전 시연에 사용하기 위함
 */

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SceneInformationRepository sceneInformationRepository;
	private final UserSceneRepository userSceneRepository;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("모든 Bean이 load된 이후에 초기 mock 데이터를 삽입합니다");

		// 초기 사용자 데이터 삽입 (이미 존재하면 스킵)
		User admin = createUserIfNotExists("admin", "admin1234!");

		// 초기 Scene 정보 데이터 삽입 (이미 존재하면 스킵)
		SceneInformation scene1 = createSceneInformationIfNotExists(1L, "로봇 팔", "Robot Arm", SceneCategory.ROBOTICS,
				45L, "심장의 펌프 작용과 혈류 역학을 3D로 학습합니다.", "https://example.com/thumb1.jpg");
		SceneInformation scene2 = createSceneInformationIfNotExists(2L, "자동차 엔진 4행정", "4-Stroke Engine Cycle", SceneCategory.AEROSPACE_ENGINEERING,
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

		log.info("삽입 완료!");
	}

	private User createUserIfNotExists(String username, String rawPassword) {
		return userRepository.findByUsername(username).orElseGet(() -> {
			User user = User.builder()
					.username(username)
					.password(passwordEncoder.encode(rawPassword))
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
}
