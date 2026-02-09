package com.blaybus.backend.dto.scene;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSceneNoteRequest {

	// 마크다운 형식의 텍스트
	@NotNull(message = "Content cannot be null")
	@Size(max = 3000, message = "Note content must be less than 3000 characters")
	private String content;

	public UserSceneNoteRequest(String content) {
		this.content = content;
	}
}
