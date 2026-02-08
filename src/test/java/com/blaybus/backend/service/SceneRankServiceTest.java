package com.blaybus.backend.service;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.SceneStatistics;
import com.blaybus.backend.dto.SceneRankResponse;
import com.blaybus.backend.repository.SceneStatisticsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SceneRankServiceTest {

    @Mock
    private SceneStatisticsRepository sceneStatisticsRepository;

    @InjectMocks
    private SceneService sceneService;

    @Test
    @DisplayName("Scene 랭킹을 1~5위까지 조회한다 (카테고리 필터 없음)")
    void getSceneRanksWithoutCategoryTest() {
        // given

        SceneInformation scene1 = SceneInformation.builder()
                .id(1L).title("로봇 팔").engTitle("Robot Arm")
                .category(SceneCategory.ROBOTICS).build();

        SceneInformation scene2 = SceneInformation.builder()
                .id(2L).title("자동차 엔진").engTitle("Car Engine")
                .category(SceneCategory.AUTOMOTIVE_ENGINEERING).build();

        SceneStatistics stat1 = SceneStatistics.builder()
                .scene(scene1).rank(1).difference(2).build();

        SceneStatistics stat2 = SceneStatistics.builder()
                .scene(scene2).rank(2).difference(-1).build();

        given(sceneStatisticsRepository.findTop5ByAggregatedTimeAndCategory(any(LocalDateTime.class), eq(null)))
                .willReturn(List.of(stat1, stat2));

        // when
        SceneRankResponse response = sceneService.getSceneRanks(null);

        // then
        assertThat(response.getScenes()).hasSize(2);
        assertThat(response.getScenes().get(0).getRank()).isEqualTo(1);
        assertThat(response.getScenes().get(0).getTitle()).isEqualTo("로봇 팔");
        assertThat(response.getScenes().get(0).getRankDiff()).isEqualTo(2);

        assertThat(response.getScenes().get(1).getRank()).isEqualTo(2);
        assertThat(response.getScenes().get(1).getTitle()).isEqualTo("자동차 엔진");
        assertThat(response.getScenes().get(1).getRankDiff()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Scene 랭킹을 카테고리별로 필터링하여 조회한다")
    void getSceneRanksWithCategoryTest() {
        // given
        SceneInformation scene1 = SceneInformation.builder()
                .id(1L).title("로봇 팔").engTitle("Robot Arm")
                .category(SceneCategory.ROBOTICS).build();

        SceneStatistics stat1 = SceneStatistics.builder()
                .scene(scene1).rank(1).difference(0).build();

        given(sceneStatisticsRepository.findTop5ByAggregatedTimeAndCategory(
                any(LocalDateTime.class), eq(SceneCategory.ROBOTICS)))
                .willReturn(List.of(stat1));

        // when
        SceneRankResponse response = sceneService.getSceneRanks(SceneCategory.ROBOTICS);

        // then
        assertThat(response.getScenes()).hasSize(1);
        assertThat(response.getScenes().get(0).getTitle()).isEqualTo("로봇 팔");
        assertThat(response.getScenes().get(0).getRank()).isEqualTo(1);
    }
}
