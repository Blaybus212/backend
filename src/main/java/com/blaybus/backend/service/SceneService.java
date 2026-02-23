package com.blaybus.backend.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.SceneStatistics;
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.dto.SceneListOrder;
import com.blaybus.backend.dto.SceneListResponse;
import com.blaybus.backend.dto.SceneRankResponse;
import com.blaybus.backend.dto.SceneResponse;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.SceneStatisticsRepository;
import com.blaybus.backend.repository.UserSceneRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneService {

	private final UserSceneRepository userSceneRepository;
	private final SceneStatisticsRepository sceneStatisticsRepository;
	private final SceneInformationRepository sceneInformationRepository;

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
							// TODO: 학습 진척도 계산 로직 구현 필요
							// - 계산 식: (클릭한 컴포넌트 개수 / 전체 컴포넌트 개수) * 100
							// - 관련 데이터: Alignment 엔티티의 클릭 상태, SceneInformation의 전체 컴포넌트 수
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

		List<SceneRankResponse.SceneRankDto> rankDtos = IntStream.range(0, statistics.size())
				.mapToObj(i -> {
					SceneStatistics stat = statistics.get(i);
					return SceneRankResponse.SceneRankDto.builder()
							.id(stat.getScene().getId().toString())
							.rank(i + 1)
							.title(stat.getScene().getTitle())
							.engTitle(stat.getScene().getEngTitle())
							.rankDiff(stat.getDifference())
							.build();
				})
				.toList();

		String todayFormatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

		return SceneRankResponse.builder()
				.today(todayFormatted)
				.scenes(rankDtos)
				.build();
	}

	public SceneListResponse getScenes(SceneCategory category, int page, int limit, String query,
			SceneListOrder order) {
		Sort sort;
		switch (order) {
			case POPULARITY:
				sort = Sort.by("participantsCount").descending();
				break;
			case ALPHABETICAL:
			default:
				sort = Sort.by("title").ascending();
				break;
		}

		PageRequest pageRequest = PageRequest.of(page - 1, limit, sort);
		Page<SceneInformation> scenePage = sceneInformationRepository.findByCategoryAndQuery(category, query,
				pageRequest);

		List<SceneListResponse.SceneDto> sceneDtos = scenePage.getContent().stream()
				.map(sceneInfo -> SceneListResponse.SceneDto.builder()
						.id(sceneInfo.getId().toString())
						.isPopular(sceneInfo
								.getParticipantsCount() >= POPULAR_SCENE_PARTICIPANTS_THRESHOLD)
						.title(sceneInfo.getTitle())
						.engTitle(sceneInfo.getEngTitle())
						.category(sceneInfo.getCategory())
						.description(sceneInfo.getDescription())
						.imageUrl(sceneInfo.getThumbnailUrl())
						.participantsCount(sceneInfo.getParticipantsCount())
						.build())
				.collect(Collectors.toList());

		return SceneListResponse.builder()
				.totalPages(scenePage.getTotalPages())
				.scenes(sceneDtos)
				.build();
	}

	/**
	 * aggregatedTime 계산 로직
	 * - 현재 시각이 07:00 이후 → 어제 07:00
	 * - 현재 시각이 07:00 이전 → 그제 07:00
	 */
	private LocalDateTime calculateAggregatedTime(LocalDateTime now) {
		if (!now.toLocalTime().isBefore(AGGREGATION_TIME)) {
			// 07:00 이후 → 어제 07:00
			return now.minusDays(1).with(AGGREGATION_TIME);
		} else {
			// 07:00 이전 → 그제 07:00
			return now.minusDays(2).with(AGGREGATION_TIME);
		}
	}

	public com.blaybus.backend.dto.SceneDetailResponse getSceneDetail(Long sceneId) {
		SceneInformation sceneInformation = sceneInformationRepository.findById(sceneId)
				.orElseThrow(() -> new IllegalArgumentException("Scene not found with id: " + sceneId));

		return com.blaybus.backend.dto.SceneDetailResponse.from(
				sceneInformation.getTitle(),
				sceneInformation.getEngTitle(),
				sceneInformation.getDescription());
	}
}
