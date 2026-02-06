package com.blaybus.backend.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 사용자 선호 테마 컬러를 정의하는 Enum
 */
public enum ThemeColor {
	BLUE("blue"),
	ORANGE("orange"),
	GREEN("green"),
	PINK("pink");

	private final String value;

	ThemeColor(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static ThemeColor fromValue(String value) {
		for (ThemeColor color : values()) {
			if (color.value.equalsIgnoreCase(value)) {
				return color;
			}
		}
		throw new IllegalArgumentException("Unknown ThemeColor: " + value);
	}
}
