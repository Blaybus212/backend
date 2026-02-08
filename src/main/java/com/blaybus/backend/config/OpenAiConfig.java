package com.blaybus.backend.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

@Configuration
public class OpenAiConfig {

	@Value("${openai.api-key}")
	private String apiKey;

	@Value("${openai.base-url}")
	private String baseUrl;

	@Value("${openai.timeout-seconds:30}")
	private int timeoutSeconds;

	@Bean
	public RestClient openAiRestClient() {
		return RestClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader("Authorization", "Bearer " + apiKey)
			.defaultHeader("Content-Type", "application/json")
			.requestFactory(clientHttpRequestFactory())
			.build();
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		return mapper;
	}

	private ClientHttpRequestFactory clientHttpRequestFactory() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
		factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
		return factory;
	}
}
