package com.blaybus.backend.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.blaybus.backend.dto.OpenAiDto.AssistantResponse;
import com.blaybus.backend.dto.OpenAiDto.ResponsesRequest;
import com.blaybus.backend.dto.OpenAiDto.ResponsesResponse;
import com.blaybus.backend.dto.OpenAiDto.SummaryResponse;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OpenAiService {

	private final RestClient openAiRestClient;
	private final ObjectMapper objectMapper;
	private final String model;
	private final int maxRetries;
	private final MeterRegistry meterRegistry;

	public OpenAiService(
		@Qualifier("openAiRestClient")
		RestClient openAiRestClient,
		@Qualifier("openAiObjectMapper")
		ObjectMapper objectMapper,
		@Value("${openai.model}")
		String model,
		@Value("${openai.max-retries:2}")
		int maxRetries,
		MeterRegistry meterRegistry) {
		this.openAiRestClient = openAiRestClient;
		this.objectMapper = objectMapper;
		this.model = model;
		this.maxRetries = maxRetries;
		this.meterRegistry = meterRegistry;
	}

	public AssistantResponse chat(String systemPrompt, String userMessage) {
		ResponsesRequest request = ResponsesRequest.of(model, systemPrompt, userMessage);

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				ResponsesResponse response = openAiRestClient.post()
					.uri("/chat/completions")
					.body(request)
					.retrieve()
					.body(ResponsesResponse.class);

				if (response == null) {
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}

				String textContent = response.getTextContent();
				if (textContent == null) {
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}

				recordTokenUsage(response, "chat");

				if (!response.isComplete()) {
					String reason = response.getFinishReason();
					log.warn("OpenAI response truncated. finish_reason={}, attempting to parse partial response",
						reason);
					// Do NOT retry — length truncation yields the same result
					return parseAssistantResponse(textContent);
				}

				return parseAssistantResponse(textContent);

			} catch (RestClientException e) {
				log.error("OpenAI API call failed (attempt {}/{}): {}", attempt + 1, maxRetries + 1, e.getMessage());
				if (attempt == maxRetries) {
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}
			}
		}

		throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
	}

	public SummaryResponse summarize(String systemPrompt, String userMessage) {
		ResponsesRequest request = ResponsesRequest.forSummary(model, systemPrompt, userMessage);

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				ResponsesResponse response = openAiRestClient.post()
					.uri("/chat/completions")
					.body(request)
					.retrieve()
					.body(ResponsesResponse.class);

				if (response == null) {
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}

				String textContent = response.getTextContent();
				if (textContent == null) {
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}

				recordTokenUsage(response, "summary");

				if (!response.isComplete()) {
					String reason = response.getFinishReason();
					log.warn(
						"OpenAI summary response truncated. finish_reason={}, attempting to parse partial response",
						reason);
					return parseSummaryResponse(textContent);
				}

				return parseSummaryResponse(textContent);

			} catch (RestClientException e) {
				log.error("OpenAI summary API call failed (attempt {}/{}): {}", attempt + 1, maxRetries + 1,
					e.getMessage());
				if (attempt == maxRetries) {
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}
			}
		}

		throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
	}

	private void recordTokenUsage(ResponsesResponse response, String type) {
		if (response.usage() == null) {
			return;
		}
		Counter.builder("openai.tokens.input")
			.tag("type", type)
			.description("OpenAI input tokens consumed")
			.register(meterRegistry)
			.increment(response.usage().inputTokens());

		Counter.builder("openai.tokens.output")
			.tag("type", type)
			.description("OpenAI output tokens consumed")
			.register(meterRegistry)
			.increment(response.usage().outputTokens());
	}

	private AssistantResponse parseAssistantResponse(String json) {
		try {
			return objectMapper.readValue(json, AssistantResponse.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to parse OpenAI response: {}", json, e);
			throw new BusinessException(CommonErrorCode.OPENAI_PARSE_ERROR);
		}
	}

	private SummaryResponse parseSummaryResponse(String json) {
		try {
			return objectMapper.readValue(json, SummaryResponse.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to parse OpenAI summary response: {}", json, e);
			throw new BusinessException(CommonErrorCode.OPENAI_PARSE_ERROR);
		}
	}
}
