package com.blaybus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.dto.OpenAiDto.AssistantResponse;
import com.blaybus.backend.service.OpenAiService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatController {

	private final OpenAiService openAiService;

	@PostMapping("/scenes/{sceneId}/chat")
	public ResponseEntity<ChatResponse> chat(
		@PathVariable("sceneId")
		Long sceneId,
		@Valid @RequestBody
		ChatRequest request) {
		String systemPrompt = buildSystemPrompt(sceneId);
		AssistantResponse response = openAiService.chat(systemPrompt, request.message());

		return ResponseEntity.ok(new ChatResponse(response.answer()));
	}

	private String buildSystemPrompt(Long sceneId) {
		return """
			당신은 SIMVEX의 공학 학습 어시스턴트입니다.
			3D 모델을 학습하는 공대생을 돕는 친근한 선배 역할을 합니다.
			한국어로 간결하게 답변하세요.
			""";
	}

	public record ChatRequest(
		@NotBlank(message = "메시지는 필수입니다.")
		String message) {
	}

	public record ChatResponse(
		String answer) {
	}
}
