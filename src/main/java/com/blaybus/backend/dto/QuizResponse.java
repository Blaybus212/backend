package com.blaybus.backend.dto;

import java.util.List;

import com.blaybus.backend.domain.quiz.QuizType;

public record QuizResponse(
	Long sceneInfoId,
	UserProgressDto userProgress,
	List<QuizItemDto> quizzes) {
	public record UserProgressDto(
		Long userProgressId,
		Long lastQuizId,
		Integer totalQuestions,
		Integer success,
		Integer failure,
		boolean isComplete) {
	}

	public record QuizItemDto(
		Long id,
		String targetPurpose,
		QuizType type,
		String question,
		String choice) {
	}
}
