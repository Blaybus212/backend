package com.blaybus.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAiDto {

	public record ResponsesRequest(
		String model,
		@JsonProperty("messages")
		List<InputMessage> input,
		@JsonProperty("response_format")
		TextFormat text,
		@JsonProperty("max_completion_tokens")
		Integer maxCompletionTokens) {
		public static ResponsesRequest of(String model, String systemPrompt, String userMessage) {
			return new ResponsesRequest(
				model,
				List.of(
					new InputMessage("system", systemPrompt),
					new InputMessage("user", userMessage)),
				TextFormat.structuredOutput(),
				2048);
		}

		public static ResponsesRequest forSummary(String model, String systemPrompt, String userMessage) {
			return new ResponsesRequest(
				model,
				List.of(
					new InputMessage("system", systemPrompt),
					new InputMessage("user", userMessage)),
				TextFormat.summaryOutput(),
				2048);
		}
	}

	public record InputMessage(
		String role,
		String content) {
	}

	public record TextFormat(
		String type,
		@JsonProperty("json_schema")
		JsonSchemaSpec jsonSchema) {

		public static TextFormat structuredOutput() {
			return new TextFormat("json_schema", JsonSchemaSpec.assistantResponse());
		}

		public static TextFormat summaryOutput() {
			return new TextFormat("json_schema", JsonSchemaSpec.summaryResponse());
		}
	}

	public record JsonSchemaSpec(
		String name,
		boolean strict,
		JsonSchemaDefinition schema) {

		public static JsonSchemaSpec assistantResponse() {
			return new JsonSchemaSpec("assistant_response", true, JsonSchemaDefinition.assistantResponse());
		}

		public static JsonSchemaSpec summaryResponse() {
			return new JsonSchemaSpec("summary_response", true, JsonSchemaDefinition.summaryResponse());
		}
	}

	public record JsonSchemaDefinition(
		String type,
		java.util.Map<String, JsonSchemaProperty> properties,
		List<String> required,
		@JsonProperty("additionalProperties")
		boolean additionalProperties) {

		public static JsonSchemaDefinition assistantResponse() {
			return new JsonSchemaDefinition(
				"object",
				java.util.Map.of(
					"answer", new JsonSchemaProperty("string", "사용자 질문에 대한 답변"),
					"summary", new JsonSchemaProperty("string", "대화 핵심 내용 1-2문장 요약")),
				List.of("answer", "summary"),
				false);
		}

		public static JsonSchemaDefinition summaryResponse() {
			return new JsonSchemaDefinition(
				"object",
				java.util.Map.of(
					"summary", new JsonSchemaProperty("string", "전체 대화 내역의 종합 요약")),
				List.of("summary"),
				false);
		}
	}

	public record JsonSchemaProperty(
		String type,
		String description) {
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

		public String getFinishReason() {
			if (output == null || output.isEmpty()) {
				return "unknown";
			}
			return output.get(0).finishReason();
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

	public record SummaryResponse(
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
