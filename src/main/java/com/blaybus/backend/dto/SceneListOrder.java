package com.blaybus.backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

public enum SceneListOrder {
    ALPHABETICAL("alphabetical", "가나다순"),
    POPULARITY("popularity", "인기순");

    /**
     * JSON 직렬화/역직렬화에 사용되는 값 (영문)
     */
    private final String value;

    /**
     * 화면 표시용 한글 명칭
     */
    @Getter
    private final String displayName;

    SceneListOrder(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SceneListOrder fromValue(String value) {
        for (SceneListOrder field : values()) {
            if (field.value.equalsIgnoreCase(value)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unknown SceneListOrder: " + value);
    }
}
