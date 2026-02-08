package com.blaybus.backend.service;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.SceneStatistics;
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.dto.SceneRankResponse;
import com.blaybus.backend.dto.SceneResponse;
import com.blaybus.backend.repository.SceneStatisticsRepository;
import com.blaybus.backend.repository.UserSceneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneService {

        private final UserSceneRepository userSceneRepository;
        private final SceneStatisticsRepository sceneStatisticsRepository;

        private static final long POPULAR_SCENE_PARTICIPANTS_THRESHOLD = 5L;
        private static final LocalTime AGGREGATION_TIME = LocalTime.of(7, 0);

        public SceneResponse getLearningScenes(Long userId) {
                List<UserScene> top3UserScenes = userSceneRepository.findTop3ByUserIdOrderByLastAccessedAtDesc(userId,
                                PageRequest.of(0, 3));

                List<SceneResponse.SceneDto> sceneDtos = top3UserScenes.stream()
                                .map(userScene -> {
                                        SceneInformation sceneInfo = userScene.getScene();
                                        return SceneResponse.SceneDto.builder()
                                                        .id(sceneInfo.getId().toString())
                                                        .title(sceneInfo.getTitle())
                                                        .engTitle(sceneInfo.getEngTitle())
                                                        .category(sceneInfo.getCategory())
                                                        .imageUrl(sceneInfo.getThumbnailUrl())
                                                        // TODO: 진척도 로직 실제 데이터 기반으로 수정 필요
                                                        .progress(35)
                                                        .popular(sceneInfo
                                                                        .getParticipantsCount() >= POPULAR_SCENE_PARTICIPANTS_THRESHOLD)
                                                        .lastAccessedAt(userScene.getLastAccessedAt())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return SceneResponse.builder()
                                .scenes(sceneDtos)
                                .build();
        }

        /**
         * 오늘 날짜의 인기 학습 오브젝트 순위 조회 (1~5위)
         * - 현재 시각이 07:00 이후 → 어제 07:00 기준 집계 데이터 사용
         * - 현재 시각이 07:00 이전 → 그제 07:00 기준 집계 데이터 사용
         */
        public SceneRankResponse getSceneRanks(SceneCategory category) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime aggregatedTime = calculateAggregatedTime(now);

                List<SceneStatistics> statistics = sceneStatisticsRepository
                                .findTop5ByAggregatedTimeAndCategory(aggregatedTime, category);

                List<SceneRankResponse.SceneRankDto> rankDtos = statistics.stream()
                                .map(stat -> SceneRankResponse.SceneRankDto.builder()
                                                .id(stat.getScene().getId().toString())
                                                .rank(stat.getRank())
                                                .title(stat.getScene().getTitle())
                                                .engTitle(stat.getScene().getEngTitle())
                                                .rankDiff(stat.getDifference())
                                                .build())
                                .collect(Collectors.toList());

                String todayFormatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                return SceneRankResponse.builder()
                                .today(todayFormatted)
                                .scenes(rankDtos)
                                .build();
        }

        /**
         * aggregatedTime 계산 로직
         * - 현재 시각이 07:00 이후 → 어제 07:00
         * - 현재 시각이 07:00 이전 → 그제 07:00
         */
        private LocalDateTime calculateAggregatedTime(LocalDateTime now) {
                if (now.toLocalTime().isAfter(AGGREGATION_TIME) || now.toLocalTime().equals(AGGREGATION_TIME)) {
                        // 07:00 이후 → 어제 07:00
                        return now.minusDays(1).with(AGGREGATION_TIME);
                } else {
                        // 07:00 이전 → 그제 07:00
                        return now.minusDays(2).with(AGGREGATION_TIME);
                }
        }
}
