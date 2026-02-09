package com.blaybus.backend.dto;

import com.blaybus.backend.domain.scene.SceneCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SceneListResponse {
    private int totalPages;
    private List<SceneDto> scenes;

    @Getter
    @Builder
    public static class SceneDto {
        private String id;
        private boolean isPopular;
        private String title;
        private String engTitle;
        private SceneCategory category;
        private String description;
        private String imageUrl;
        private long participantsCount;
    }
}
