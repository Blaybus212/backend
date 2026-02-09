package com.blaybus.backend.service;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.dto.SceneListOrder;
import com.blaybus.backend.dto.SceneListResponse;
import com.blaybus.backend.dto.SceneResponse;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.UserSceneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SceneServiceTest {

	@Mock
	private UserSceneRepository userSceneRepository;

	@Mock
	private SceneInformationRepository sceneInformationRepository;

	@InjectMocks
	private SceneService sceneService;

	@Test
	@DisplayName("사용자의 학습 중인 Scene 3개를 최신순으로 가져온다.")
	void getLearningScenesTest() {
		// given

		SceneInformation scene1 = SceneInformation.builder()
			.id(1L).title("로봇 팔").engTitle("Robot Arm")
			.category(SceneCategory.AEROSPACE_ENGINEERING)
			.participantsCount(10L).thumbnailUrl("url1").build();

		SceneInformation scene2 = SceneInformation.builder()
			.id(2L).title("심장 전문").engTitle("Heart").category(SceneCategory.ROBOTICS)
			.participantsCount(3L).thumbnailUrl("url2").build();

		UserScene us1 = UserScene.builder().scene(scene1).lastAccessedAt(LocalDateTime.now()).build();
		UserScene us2 = UserScene.builder().scene(scene2).lastAccessedAt(LocalDateTime.now().minusDays(1))
			.build();

		given(userSceneRepository.findTop3ByUserIdOrderByLastAccessedAtDesc(eq(1L), any(PageRequest.class)))
			.willReturn(List.of(us1, us2));

		// when
		SceneResponse response = sceneService.getLearningScenes(1L);

		// then
		assertThat(response.getScenes()).hasSize(2);

		// Scene 1 check (Popular)
		assertThat(response.getScenes().get(0).getTitle()).isEqualTo("로봇 팔");
		assertThat(response.getScenes().get(0).isPopular()).isTrue();
		assertThat(response.getScenes().get(0).getProgress()).isEqualTo(35);

		// Scene 2 check (Not Popular)
		assertThat(response.getScenes().get(1).getTitle()).isEqualTo("심장 전문");
		assertThat(response.getScenes().get(1).isPopular()).isFalse();
		assertThat(response.getScenes().get(1).getProgress()).isEqualTo(35);
	}

	@Test
	@DisplayName("Scene 리스트를 필터링 및 페이징하여 가져온다.")
	void getScenesTest() {
		// given
		SceneInformation scene1 = SceneInformation.builder()
			.id(1L).title("A Scene").engTitle("A Scene Eng")
			.category(SceneCategory.ROBOTICS)
			.participantsCount(10L).build();

		Page<SceneInformation> page = new PageImpl<>(List.of(scene1),
			PageRequest.of(0, 9, Sort.by("title").ascending()), 1);

		given(sceneInformationRepository.findByCategoryAndQuery(eq(SceneCategory.ROBOTICS), eq("A"),
			any(PageRequest.class)))
			.willReturn(page);

		// when
		SceneListResponse response = sceneService.getScenes(SceneCategory.ROBOTICS, 1, 9, "A",
			SceneListOrder.ALPHABETICAL);

		// then
		assertThat(response.getScenes()).hasSize(1);
		assertThat(response.getScenes().get(0).getTitle()).isEqualTo("A Scene");
		assertThat(response.getTotalPages()).isEqualTo(1);
	}
}
