package com.blaybus.backend.domain.quiz;

import jakarta.persistence.*;
import lombok.*;

import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Builder
@Entity
@Table(name = "quiz_user_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "scene_info_id" })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizUserProgress {

    /**
     * 자체 식별자 (surrogate key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_info_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SceneInformation scene;

    /**
     * 사용자가 마지막으로 보고 있던 퀴즈 ID
     * - 재접속 시 이어서 풀기 위한 포인터 역할
     */
    @Column(name = "last_quiz_id")
    private Long lastQuizId;

    /**
     * 해당 퀴즈 세션의 전체 문항 수
     * - 퀴즈 생성 시 고정
     */
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    /**
     * 현재 퀴즈 세션에서 정답 횟수
     */
    @Column(name = "success", nullable = false)
    private Integer success;

    /**
     * 현재 퀴즈 세션에서 오답 횟수
     */
    @Column(name = "failure", nullable = false)
    private Integer failure;

    @Column(name = "is_complete", nullable = false)
    private boolean isComplete;

    /**
     * 퀴즈 풀이에 소요된 시간
     * - 단위: 초(seconds)
     * - 누적 시간
     */
    @Column(name = "solve_time", nullable = false)
    private Integer solveTime;
}
