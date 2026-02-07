package com.blaybus.backend.domain.quiz;

import jakarta.persistence.*;
import lombok.*;

import com.blaybus.backend.domain.scene.SceneInformation;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Builder
@Entity
@Table(name = "scene_ranks_quiz")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SceneRanksQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 퀴즈가 속한 scene 식별자
     * - scene_info 테이블의 PK
     * - 연관관계 없이 숫자 값으로만 관리
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_info_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SceneInformation scene;

    /**
     * 퀴즈의 목적
     * - 퀴즈 분류/난이도/학습 단계 판단용 메타데이터
     * 예시:
     * - "명칭 및 구조 파악"
     * - "작동 원리 및 메커니즘"
     * - "AI 대화 기반 나의 취약점 공략"
     */
    @Column(name = "target_purpose", length = 30, nullable = false)
    private String targetPurpose;

    /**
     * 문제 유형
     * - SELECT : 객관식
     * - INPUT : 주관식
     * → 프론트 렌더링 방식과 채점 로직 분기 기준
     */
    @Column(name = "type", length = 10, nullable = false)
    private String type;

    /**
     * 문제 본문
     * - 특정 scene을 보고 풀어야 하는 질문
     * - scene context + 자연어 질문
     */
    @Column(name = "question", length = 255, nullable = false)
    private String question;

    /**
     * 정답 데이터
     * - SELECT:
     * 예) JSON / CSV / delimiter 기반 문자열
     * ["A","C"] 또는 "A|C"
     * - INPUT:
     * 정답 문자열 또는 키워드 기준
     * → 채점 로직은 애플리케이션 레벨에서 처리
     */
    @Column(name = "answer", length = 300, nullable = false)
    private String answer;
}
