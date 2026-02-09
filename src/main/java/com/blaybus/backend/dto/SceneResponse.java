package com.blaybus.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.blaybus.backend.domain.scene.SceneCategory;

import lombok.Builder;
import lombok.Getter;

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
		private SceneCategory category;
		private String imageUrl;
		private int progress;
		private boolean popular;
		private LocalDateTime lastAccessedAt;
	}
}
