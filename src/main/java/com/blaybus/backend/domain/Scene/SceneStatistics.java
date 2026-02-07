package com.blaybus.backend.domain.scene;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
/**
 * SceneStatistics
 * - batch 처리로 생성되는 통계 스냅샷 테이블
 * - 특정 기간(07:00 기준)에 대해 scene 단위로 집계된 결과를 저장
 * - 실시간 계산이 아닌 "집계 시점의 결과"를 그대로 보존하는 목적
 */
@Entity
@Table(name = "scene_statistics")
public class SceneStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

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
}
