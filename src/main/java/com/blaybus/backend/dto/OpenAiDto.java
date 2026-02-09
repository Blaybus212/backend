package com.blaybus.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAiDto {

	public record ResponsesRequest(
		String model,
		@JsonProperty("messages")
		List<InputMessage> input,
		@JsonProperty("response_format")
		TextFormat text) {
		public static ResponsesRequest of(String model, String systemPrompt, String userMessage) {
			return new ResponsesRequest(
				model,
				List.of(
					new InputMessage("system", systemPrompt),
					new InputMessage("user", userMessage)),
				TextFormat.jsonObject());
		}
	}

	public record InputMessage(
		String role,
		String content) {
	}

	public record TextFormat(
		String type) {
		public static TextFormat jsonObject() {
			return new TextFormat("json_object");
		}
	}

	public record ResponsesResponse(
		String id,
		String object,
		@JsonProperty("choices")
		List<OutputItem> output,
		Usage usage) {

		public String getTextContent() {
			if (output == null || output.isEmpty()) {
				return null;
			}
			return output.get(0).message().content();
		}

		public boolean isComplete() {
			if (output == null || output.isEmpty()) {
				return false;
			}
			String finishReason = output.get(0).finishReason();
			return "stop".equals(finishReason);
		}
	}

	public record OutputItem(
		int index,
		InputMessage message,
		@JsonProperty("finish_reason")
		String finishReason) {
	}

	public record Usage(
		@JsonProperty("prompt_tokens")
		int promptTokens,
		@JsonProperty("completion_tokens")
		int completionTokens,
		@JsonProperty("total_tokens")
		int totalTokens) {

		public int inputTokens() {
			return promptTokens;
		}

		public int outputTokens() {
			return completionTokens;
		}
	}

	public record AssistantResponse(
		String answer,
		String summary) {
	}

	public record EmbeddingRequest(
		String input,
		String model) {
	}

	public record EmbeddingResponse(
		List<EmbeddingData> data,
		Usage usage) {

		public record EmbeddingData(List<Double> embedding) {
		}

		public record Usage(
			@JsonProperty("prompt_tokens")
			int promptTokens,
			@JsonProperty("total_tokens")
			int totalTokens) {
		}
	}
}
