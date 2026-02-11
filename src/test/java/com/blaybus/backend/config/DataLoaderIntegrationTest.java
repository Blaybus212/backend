package com.blaybus.backend.config;

import com.blaybus.backend.domain.alignment.Component;
import com.blaybus.backend.repository.ComponentRepository;
import com.blaybus.backend.repository.QuizRepository;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.SceneStatisticsRepository;
import com.blaybus.backend.repository.UserGrassRepository;
import com.blaybus.backend.repository.UserSceneRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataLoaderIntegrationTest {

	@Autowired
	private ComponentRepository componentRepository;

	@Autowired
	private SceneInformationRepository sceneRepository;

	@Autowired
	private com.blaybus.backend.repository.UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ResourcePatternResolver resourcePatternResolver;

	@Autowired
	private UserSceneRepository userSceneRepository;

	@Autowired
	private SceneStatisticsRepository sceneStatisticsRepository;

	@Autowired
	private UserGrassRepository userGrassRepository;

	@Autowired
	private QuizRepository quizRepository;

	@Test
	@DisplayName("Verify initial scenes and components are loaded from JSON")
	void testInitialDataLoaded() throws Exception {
		// DataLoader는 @Profile("!test")로 설정되어 있어서 test 프로파일에서는 Bean이 생성되지 않습니다.
		// 따라서 테스트 프로파일에서도 실행되도록 DataLoader를 직접 생성하여 실행합니다.
		DataLoader dataLoader = new DataLoader(
			userRepository,
			passwordEncoder,
			componentRepository,
			objectMapper,
			resourcePatternResolver,
			sceneRepository,
			sceneStatisticsRepository,
			userGrassRepository,
			quizRepository);

		// DataLoader 실행
		dataLoader.run(null);

		// 1. Scene 데이터 검증
		assertThat(sceneRepository.findByEngTitle("Quadcopter Drone")).isPresent();
		assertThat(sceneRepository.findByEngTitle("Leaf Spring")).isPresent();
		assertThat(sceneRepository.findByEngTitle("Robotic Gripper")).isPresent();
		assertThat(sceneRepository.findByEngTitle("Vehicle Suspension")).isPresent();
		assertThat(sceneRepository.findByEngTitle("Machine Tool Vise")).isPresent();

		// 2. Component 데이터 검증 (Drone Scene)
		Optional<Component> armGear = componentRepository.findByName("arm_gear");
		assertThat(armGear).isPresent();
		assertThat(armGear.get().getDescription()).contains("암 구동부").contains("기어 모듈");
		assertThat(armGear.get().getUsage()).contains("구동");

		// 3. Component 데이터 검증 (Leaf Spring Scene)
		// 실제 매핑된 Asset ID 확인 필요하지만, 일단 존재 여부만 체크
		List<Component> allComponents = componentRepository.findAll();
		assertThat(allComponents).isNotEmpty();
		System.out.println("Loaded components count: " + allComponents.size());
	}
}
