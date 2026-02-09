package com.blaybus.backend.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.blaybus.backend.dto.OpenAiDto.AssistantResponse;
import com.blaybus.backend.dto.OpenAiDto.ResponsesRequest;
import com.blaybus.backend.dto.OpenAiDto.ResponsesResponse;
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
					.uri("/responses")
					.body(request)
					.retrieve()
					.body(ResponsesResponse.class);

				if (response == null) {
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}

				if (!response.isComplete()) {
					log.warn("OpenAI response incomplete: {}", response.incompleteDetails());
					if (response.incompleteDetails() != null
						&& "max_output_tokens".equals(response.incompleteDetails().reason())) {
						throw new BusinessException(CommonErrorCode.OPENAI_TOKEN_EXCEEDED);
					}
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}

				String textContent = response.getTextContent();
				if (textContent == null) {
					throw new BusinessException(CommonErrorCode.OPENAI_API_ERROR);
				}

				// Record token usage metrics
				if (response.usage() != null) {
					Counter.builder("openai.tokens.input")
						.description("OpenAI input tokens consumed")
						.register(meterRegistry)
						.increment(response.usage().inputTokens());

					Counter.builder("openai.tokens.output")
						.description("OpenAI output tokens consumed")
						.register(meterRegistry)
						.increment(response.usage().outputTokens());
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

	private AssistantResponse parseAssistantResponse(String json) {
		try {
			return objectMapper.readValue(json, AssistantResponse.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to parse OpenAI response: {}", json, e);
			throw new BusinessException(CommonErrorCode.OPENAI_PARSE_ERROR);
		}
	}
}
