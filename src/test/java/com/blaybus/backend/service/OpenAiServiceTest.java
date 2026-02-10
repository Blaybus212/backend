package com.blaybus.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.blaybus.backend.dto.OpenAiDto.AssistantResponse;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ActiveProfiles("test")
@SpringBootTest(classes = {OpenAiService.class, OpenAiServiceTest.TestConfig.class}, properties = {
	"openai.model=gpt-4o-mini",
	"openai.max-retries=2"
})
class OpenAiServiceTest {

	@Autowired
	private OpenAiService openAiService;

	@Autowired
	private MockRestServiceServer mockServer;

	@TestConfiguration
	static class TestConfig {
		@Bean
		public RestClient.Builder restClientBuilder() {
			return RestClient.builder();
		}

		@Bean
		public MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
			return MockRestServiceServer.bindTo(builder).build();
		}

		@Bean
		@Qualifier("openAiRestClient")
		public RestClient openAiRestClient(RestClient.Builder builder, MockRestServiceServer server) {
			return builder
				.baseUrl("https://api.openai.com/v1")
				.build();
		}

		@Bean
		@Qualifier("openAiObjectMapper")
		public ObjectMapper openAiObjectMapper() {
			ObjectMapper mapper = new ObjectMapper();
			mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
			return mapper;
		}

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}
	}

	@BeforeEach
	void setUp() {
		mockServer.reset();
	}

	@Test
	@DisplayName("OpenAI API 호출 성공 - 정상 응답을 반환한다")
	void chat_success() {
		// given
		String systemPrompt = "시스템 프롬프트";
		String userMessage = "사용자 메시지";
		String mockResponse = """
			{
			    "id": "chatcmpl-123",
			    "object": "chat.completion",
			    "created": 1677652288,
			    "model": "gpt-4o-mini",
			    "choices": [{
			        "index": 0,
			        "message": {
			            "role": "assistant",
			            "content": "{\\"answer\\": \\"답변입니다\\", \\"summary\\": \\"요약입니다\\"}"
			        },
			        "finish_reason": "stop"
			    }],
			    "usage": {
			        "prompt_tokens": 9,
			        "completion_tokens": 12,
			        "total_tokens": 21
			    }
			}
			""";

		mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
			.andExpect(method(POST))
			.andRespond(withSuccess(mockResponse, APPLICATION_JSON));

		// when
		AssistantResponse response = openAiService.chat(systemPrompt, userMessage);

		// then
		assertThat(response).isNotNull();
		assertThat(response.answer()).isEqualTo("답변입니다");
		assertThat(response.summary()).isEqualTo("요약입니다");
	}

	@Test
	@DisplayName("OpenAI API 호출 실패 - 503 에러 발생 시 BusinessException을 던진다")
	void chat_fail_503() {
		// given
		String systemPrompt = "시스템 프롬프트";
		String userMessage = "사용자 메시지";

		// 재시도 횟수(2회) + 최초 시도(1회) = 총 3회 호출 예상
		for (int i = 0; i < 3; i++) {
			mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
				.andExpect(method(POST))
				.andRespond(withServerError());
		}

		// when & then
		assertThatThrownBy(() -> openAiService.chat(systemPrompt, userMessage))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(CommonErrorCode.OPENAI_API_ERROR);
	}
}
