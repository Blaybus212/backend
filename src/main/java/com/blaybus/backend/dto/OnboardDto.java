package com.blaybus.backend.dto;

import com.blaybus.backend.domain.user.EducationLevel;
import com.blaybus.backend.domain.user.Persona;
import com.blaybus.backend.domain.user.ThemeColor;

public class OnboardDto {

	private OnboardDto() {}

	public record OnboardRequest(
		String name,
		String preferCategory,
		EducationLevel educationLevel,
		String specialized,
		Persona persona,
		ThemeColor themeColor) {
	}
}
