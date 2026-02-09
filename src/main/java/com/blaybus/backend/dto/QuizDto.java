package com.blaybus.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class QuizDto {

	public record GradeRequest(@NotBlank
	String answer) {
	}

	public record GradeResponse(
		boolean correct,
		double score,
		String correctAnswer) {
	}

	public record SyncProgressRequest(
		Long lastQuizId,
		Integer totalQuestions,
		Integer success,
		Integer failure,
		Integer solveTime,
		boolean isComplete) {
	}
}
