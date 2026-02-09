package com.blaybus.backend.domain.quiz;

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

@Getter
@Builder
@Entity
@Table(name = "scene_ranks_quiz_choice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SceneRanksQuizChoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 소속된 퀴즈 식별자
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "quiz_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private SceneRanksQuiz quiz;

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
