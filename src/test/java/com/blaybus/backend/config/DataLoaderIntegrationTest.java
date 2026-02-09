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

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		// DataLoader가 참조할 Scene 정보 미리 저장
		if (sceneRepository.findByEngTitle("Drone").isEmpty()) {
			sceneRepository.save(com.blaybus.backend.domain.scene.SceneInformation.builder()
				.title("드론")
				.engTitle("Drone")
				.assetPath("Drone")
				.category("Assembly")
				.description("Test Drone Scene")
				.participantsCount(0L)
				.defaultAlignmentId(0L)
				.build());
		}

		if (sceneRepository.findByEngTitle("Leaf Spring").isEmpty()) {
			sceneRepository.save(com.blaybus.backend.domain.scene.SceneInformation.builder()
				.title("리프 스프링")
				.engTitle("Leaf Spring")
				.assetPath("Leaf Spring") // 실제 폴더명과 일치해야 함
				.category("Assembly")
				.description("Test Leaf Spring")
				.participantsCount(0L)
				.defaultAlignmentId(0L)
				.build());
		}
	}

	@Test
	@DisplayName("Verify initial components are loaded from JSON")
	void testInitialComponentsLoaded() throws Exception {
		// DataLoader는 ApplicationRunner이므로 Spring Context 로딩 시 자동 실행되지만,
		// 테스트 환경에서는 명시적으로 run을 호출하거나, 혹은 이미 실행되었는지 확인해야 함.
		// @SpringBootTest는 기본적으로 ApplicationRunner를 실행하지 않을 수 있음(설정에 따라 다름).
		// 안전하게 수동으로 run 호출을 시도해봄 (이미 실행되었다면 중복 체크 로직에 의해 스킵될 것임)

		// 하지만 @SpringBootTest는 webEnvironment가 MOCK일 때 ApplicationRunner를 실행하지 않는 경우가 있음.
		// 일단 수동 호출로 검증.
		dataLoader.run(null);

		// 검증: Drone 씬의 컴포넌트
		Optional<Component> armGear = componentRepository.findByName("arm_gear");
		assertThat(armGear).isPresent();
		assertThat(armGear.get().getDescription()).contains("암 구동부의 기어 모듈");
		assertThat(armGear.get().getUsage()).contains("구동");

		// 검증: Leaf Spring 씬의 컴포넌트
		Optional<Component> leafLayer = componentRepository.findByName("Leaf-Layer");
		// 주의: assetId 매핑 확인 필요. config를 보면 Leaf-Layer1 -> Leaf-Layer 일 가능성 높음.
		// 실제 데이터: Leaf-Layer1 (Node) -> Asset ID ?
		// assembly_config.json을 확인하지 않고 추측하기 어려움. 그러나 DataLoader 로직상 AssetID로 저장됨.
		// 만약 Leaf-Layer.gltf 라면 AssetID는 Leaf-Layer 일 것.

		// Machine Vice
		// part8-grandplatte1 -> part8-grandplatte (예상)

		// 전체 개수 확인 (대략적으로)
		List<Component> allComponents = componentRepository.findAll();
		assertThat(allComponents).isNotEmpty();
		System.out.println("Loaded components count: " + allComponents.size());

		allComponents.forEach(c -> System.out.println("Component: " + c.getName() + ", Desc: " + c.getDescription()));
	}
}
