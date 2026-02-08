package com.blaybus.backend.domain.scene;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 공학 분야를 정의하는 Enum
 */
public enum SceneCategory {

    ROBOTICS("robotics", "로봇공학"),
    AUTOMOTIVE_ENGINEERING("automotive_engineering", "자동차공학"),
    AEROSPACE_ENGINEERING("aerospace_engineering", "항공우주공학"),
    MANUFACTURING_ENGINEERING("manufacturing_engineering", "제조공학");

    /**
     * JSON 직렬화/역직렬화에 사용되는 값 (영문)
     */
    private final String value;

    /**
     * 화면 표시용 한글 명칭
     */
    @Getter
    private final String displayName;

    SceneCategory(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SceneCategory fromValue(String value) {
        for (SceneCategory field : values()) {
            if (field.value.equalsIgnoreCase(value)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unknown SceneCategory: " + value);
    }
}
