package com.blaybus.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SceneRankResponse {
    private String today; // yyyy-MM-dd HH:mm
    private List<SceneRankDto> scenes;

    @Getter
    @Builder
    public static class SceneRankDto {
        private String id;
        private Integer rank;
        private String title;
        private String engTitle;
        private Integer rankDiff;
    }
}
