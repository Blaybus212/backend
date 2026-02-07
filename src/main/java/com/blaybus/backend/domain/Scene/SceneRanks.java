package com.blaybus.backend.domain.scene;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Getter
@Builder
@Entity
@Table(name = "scene_ranks", uniqueConstraints = {
        /**
         * 하루에 하나의 랭킹 결과만 존재해야 함
         * → 동일 aggregate_date에 중복 집계 방지
         */
        @UniqueConstraint(columnNames = { "aggregate_date", "category" })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SceneRanks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 랭킹이 산정된 기준 날짜
     * 예: 2026-02-07
     * - 통계 집계 기준일
     */
    @Column(name = "aggregate_date", nullable = false)
    private LocalDate aggregateDate;

    /**
     * TOTAL, ...
     */

    @Column(name = "category", nullable = false)
    private String category;

    /**
     * 랭킹 결과 및 대시보드용 집계 데이터
     * 예시 구조 (가변):
     * {
     * "topScenes": [
     * { "sceneId": 1, "rank": 1, "score": 123.4 , "difference" : '+3'},
     * { "sceneId": 7, "rank": 2, "score": 118.9 , "difference" : '+3'}
     * ],
     * "totalSceneCount": 42
     * }
     * - 구조가 자주 바뀔 가능성이 높음
     * - JOIN 없이 바로 API 응답으로 내려주기 위함
     */
    @Column(name = "result", columnDefinition = "jsonb", nullable = false)
    private String result;

    /**
     * 이전 기간에서 랭킹과의 차이점
     * 규칙 예시:
     * +3 → 순위 상승
     * -2 → 순위 하락
     * 0 → 변화 없음
     * ※ "랭킹 전체의 평균 변화" 또는
     * "1위 기준 변화" 등 해석은 비즈니스 룰에 따라 결정
     */
    @Column(name = "difference", nullable = false)
    private Integer difference;
}
