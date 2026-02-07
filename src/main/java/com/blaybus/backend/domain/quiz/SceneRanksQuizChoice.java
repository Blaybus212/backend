package com.blaybus.backend.domain.quiz;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@Table(name = "scene_ranks_quiz_choice")
public class SceneRanksQuizChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속된 퀴즈 식별자
     */
    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    /**
     * 화면 렌더링 순서
     */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /**
     * 선택지 내용
     */
    @Column(name = "content", length = 255, nullable = false)
    private String content;
}
