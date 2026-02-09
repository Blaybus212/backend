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
}
