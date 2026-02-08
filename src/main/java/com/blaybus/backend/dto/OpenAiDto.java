package com.blaybus.backend.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAiDto {

	public record ResponsesRequest(
		String model,
		List<InputMessage> input,
		TextFormat text) {
		public static ResponsesRequest of(String model, String systemPrompt, String userMessage) {
			return new ResponsesRequest(
				model,
				List.of(
					new InputMessage("system", systemPrompt),
					new InputMessage("user", userMessage)),
				TextFormat.structuredOutput());
		}
	}

	public record InputMessage(
		String role,
		String content) {
	}

	public record TextFormat(
		Format format) {
		public static TextFormat structuredOutput() {
			return new TextFormat(Format.assistantResponse());
		}
	}

	public record Format(
		String type,
		String name,
		boolean strict,
		Map<String, Object> schema) {
		public static Format assistantResponse() {
			return new Format(
				"json_schema",
				"assistant_response",
				true,
				Map.of(
					"type", "object",
					"properties", Map.of(
						"answer", Map.of("type", "string"),
						"summary", Map.of("type", "string")),
					"required", List.of("answer", "summary"),
					"additionalProperties", false));
		}
	}

	public record ResponsesResponse(
		String id,
		String status,
		List<OutputItem> output,
		@JsonProperty("incomplete_details")
		IncompleteDetails incompleteDetails) {
		public boolean isComplete() {
			return "completed".equals(status);
		}

		public String getTextContent() {
			if (output == null || output.isEmpty()) {
				return null;
			}
			return output.stream()
				.filter(item -> "message".equals(item.type()))
				.flatMap(item -> item.content().stream())
				.filter(content -> "output_text".equals(content.type()))
				.map(Content::text)
				.findFirst()
				.orElse(null);
		}
	}

	public record OutputItem(
		String type,
		String role,
		List<Content> content) {
	}

	public record Content(
		String type,
		String text) {
	}

	public record IncompleteDetails(
		String reason) {
	}

	public record AssistantResponse(
		String answer,
		String summary) {
	}
}
