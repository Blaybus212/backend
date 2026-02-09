package com.blaybus.backend.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.blaybus.backend.dto.SceneListOrder;

/**
 * Spring MVC에서 @RequestParam으로 전달된 String 값을 SceneListOrder enum으로 변환하는 컨버터
 */
@Component
public class SceneListOrderConverter implements Converter<String, SceneListOrder> {

    @Override
    public SceneListOrder convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return SceneListOrder.fromValue(source);
    }
}
