package com.blaybus.backend.dto.scene;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSceneNoteResponse {

	private String content;

	public static UserSceneNoteResponse of(String content) {
		return UserSceneNoteResponse.builder()
			.content(content)
			.build();
	}
}
