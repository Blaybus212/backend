package com.blaybus.backend.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 사용자 교육 수준을 정의하는 Enum
 */
public enum EducationLevel {
	BEGINNER("beginner"),
	FUNDAMENTAL("fundamental"),
	INTERMEDIATE("intermediate"),
	EXPERT("expert");

	private final String value;

	EducationLevel(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static EducationLevel fromValue(String value) {
		for (EducationLevel level : values()) {
			if (level.value.equalsIgnoreCase(value)) {
				return level;
			}
		}
		throw new IllegalArgumentException("Unknown EducationLevel: " + value);
	}
}
