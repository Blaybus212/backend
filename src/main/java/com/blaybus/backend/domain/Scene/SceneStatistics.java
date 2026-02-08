package com.blaybus.backend.domain.scene;

<<<<<<< HEAD
import jakarta.persistence.AllArgsConstructor;
import jakarta.persistence.Builder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
=======
import java.time.LocalDateTime;

>>>>>>> 278a307 (style(entity): fix checkstyle import violations)
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SceneStatistics
 * - batch 처리로 생성되는 통계 스냅샷 테이블
 * - 특정 기간(07:00 기준)에 대해 scene 단위로 집계된 결과를 저장
 * - 실시간 계산이 아닌 "집계 시점의 결과"를 그대로 보존하는 목적
 */
@Getter
@Builder
@Entity
@Table(name = "scene_statistics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SceneStatistics {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scene_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private SceneInformation scene;

	/**
	 * 집계 기준 시각
	 * 규칙:
	 * - 현재 시각이 07:00 이후 → 금일 07:00 기준 집계 데이터 사용
	 * - 현재 시각이 07:00 이전 → 전일 07:00 기준 집계 데이터 사용
	 * 즉, 이 컬럼은 "통계가 대표하는 기준 시점"을 의미함
	 */
	@Column(name = "aggregated_time", nullable = false)
	private LocalDateTime aggregatedTime;

	/**
	 * 해당 기간 동안 scene에 누적된 점수
	 * - score 산정 방식은 "집계 당시의 비즈니스 룰"을 따름
	 * - 이후 룰이 변경되더라도 과거 통계 값은 변경하지 않음
	 * - 통계 스냅샷의 불변성을 보장하기 위함
	 */
	@Column(name = "score", nullable = false)
	private Integer score;

	/**
	 * 오늘 하루 각 scene 의 랭킹
	 * - score 내림차순
	 */
	@Column(name = "rank")
	private Integer rank;

	/**
	 * 전날 랭킹과 차이
	 */
	@Column(name = "difference")
	private Integer difference;
}
