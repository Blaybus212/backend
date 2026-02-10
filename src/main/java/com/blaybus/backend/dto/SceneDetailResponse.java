package com.blaybus.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SceneDetailResponse {
	private String title;
	private String engTitle;
	private String description;
	private Boolean isSceneInformation;

	public static SceneDetailResponse from(String title, String engTitle, String description) {
		return SceneDetailResponse.builder()
			.title(title)
			.engTitle(engTitle)
			.description(description)
			.isSceneInformation(true) // 클라이언트에서 렌더링 화면에서의 특정 처리를 위함
			.build();
	}
}
