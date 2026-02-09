package com.blaybus.backend.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * AI 응답 어조를 정의하는 Enum
 */
public enum Persona {
	SENIOR("senior"),
	PROFESSOR("professor"),
	FRIEND("friend"),
	ASSISTANT("assistant");

	private final String value;

	Persona(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static Persona fromValue(String value) {
		String normalizedInput = normalize(value);
		for (Persona persona : values()) {
			if (normalize(persona.value).equals(normalizedInput)
				|| normalize(persona.name()).equals(normalizedInput)) {
				return persona;
			}
		}
		throw new IllegalArgumentException("Unknown Persona: " + value);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.replace("-", "").replace("_", "").replace(" ", "").toLowerCase();
	}
}
