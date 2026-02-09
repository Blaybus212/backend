package com.blaybus.backend.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.blaybus.backend.domain.scene.SceneCategory;

/**
 * Spring MVC에서 @RequestParam으로 전달된 String 값을 SceneCategory enum으로 변환하는 컨버터
 */
@Component
public class SceneCategoryConverter implements Converter<String, SceneCategory> {

    @Override
    public SceneCategory convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return SceneCategory.fromValue(source);
    }
}
