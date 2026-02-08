package com.blaybus.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SceneResponse {
    private List<SceneDto> scenes;

    @Getter
    @Builder
    public static class SceneDto {
        private String id;
        private String title;
        private String engTitle;
        private String category;
        private String imageUrl;
        private int progress;
        private boolean popular;
    }
}
