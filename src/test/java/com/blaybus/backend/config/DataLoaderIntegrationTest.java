package com.blaybus.backend.config;

import com.blaybus.backend.domain.alignment.Component;
import com.blaybus.backend.repository.ComponentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
	private DataLoader dataLoader;

	@Autowired
	private com.blaybus.backend.repository.SceneInformationRepository sceneRepository;

	@Test
	@DisplayName("Verify initial scenes and components are loaded from JSON")
	void testInitialDataLoaded() throws Exception {
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
		assertThat(armGear.get().getDescription()).contains("암 구동부의 기어 모듈");
		assertThat(armGear.get().getUsage()).contains("구동");

		// 3. Component 데이터 검증 (Leaf Spring Scene)
		// 실제 매핑된 Asset ID 확인 필요하지만, 일단 존재 여부만 체크
		List<Component> allComponents = componentRepository.findAll();
		assertThat(allComponents).isNotEmpty();
		System.out.println("Loaded components count: " + allComponents.size());
	}
}
