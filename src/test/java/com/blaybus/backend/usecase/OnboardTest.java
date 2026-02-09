package com.blaybus.backend.usecase;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.security.JwtTokenProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E2E 온보딩 기능 테스트
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class OnboardTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	private ObjectMapper objectMapper;

	private User testUser;
	private String validToken;

	@BeforeEach
	void init() {
		objectMapper = new ObjectMapper();
		testUser = User.builder()
			.username(UUID.randomUUID().toString())
			.password(passwordEncoder.encode("password123"))
			.name(null)
			.onBoardingCompleted(false)
			.build();
		userRepository.save(testUser);

		// 유효한 JWT 토큰 생성
		validToken = jwtTokenProvider.createToken(testUser.getUsername());
	}

	@Nested
	@DisplayName("온보딩 성공 테스트")
	class OnboardSuccessTest {

		@Test
		@DisplayName("온보딩 성공 - 204 No Content 반환")
		void onboard_Success() throws Exception {
			var request = Map.of(
				"name", "홍길동",
				"preferCategory", "aerospace_engineering",
				"educationLevel", "beginner",
				"specialized", "전기공학",
				"persona", "friend",
				"themeColor", "blue");

			mockMvc.perform(patch("/onboard")
				.header("Authorization", "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isNoContent());
		}
	}

	@Nested
	@DisplayName("온보딩 실패 테스트")
	class OnboardFailTest {

		@Test
		@DisplayName("온보딩 실패 - 인증 토큰 없음 (401 Unauthorized)")
		void onboard_Fail_Unauthorized() throws Exception {
			var request = Map.of(
				"name", "홍길동",
				"preferCategory", "기계공학",
				"educationLevel", "beginner",
				"specialized", "전기공학",
				"persona", "friend",
				"themeColor", "blue");

			mockMvc.perform(patch("/onboard")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("온보딩 실패 - 만료된 토큰 (401 Unauthorized)")
		void onboard_Fail_ExpiredToken() throws Exception {
			// 만료된 토큰 (실제로는 JwtTokenProvider에서 만료 시간을 설정해야 함)
			String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTYwMDAwMDAwMCwiZXhwIjoxNjAwMDAwMDAxfQ.invalid";

			var request = Map.of(
				"name", "홍길동",
				"preferCategory", "기계공학",
				"educationLevel", "beginner",
				"specialized", "전기공학",
				"persona", "friend",
				"themeColor", "blue");

			mockMvc.perform(patch("/onboard")
				.header("Authorization", "Bearer " + expiredToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("온보딩 실패 - 잘못된 Persona 값 (400 Bad Request)")
		void onboard_Fail_InvalidPersona() throws Exception {
			var request = Map.of(
				"name", "홍길동",
				"preferCategory", "기계공학",
				"educationLevel", "beginner",
				"specialized", "전기공학",
				"persona", "invalid_persona",
				"themeColor", "blue");

			mockMvc.perform(patch("/onboard")
				.header("Authorization", "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CommonErrorCode.INVALID_PARAMETER"));
		}

		@Test
		@DisplayName("온보딩 실패 - 잘못된 ThemeColor 값 (400 Bad Request)")
		void onboard_Fail_InvalidThemeColor() throws Exception {
			var request = Map.of(
				"name", "홍길동",
				"preferCategory", "기계공학",
				"educationLevel", "beginner",
				"specialized", "전기공학",
				"persona", "friend",
				"themeColor", "red");

			mockMvc.perform(patch("/onboard")
				.header("Authorization", "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CommonErrorCode.INVALID_PARAMETER"));
		}

		@Test
		@DisplayName("온보딩 실패 - 잘못된 EducationLevel 값 (400 Bad Request)")
		void onboard_Fail_InvalidEducationLevel() throws Exception {
			var request = Map.of(
				"name", "홍길동",
				"preferCategory", "기계공학",
				"educationLevel", "advanced",
				"specialized", "전기공학",
				"persona", "friend",
				"themeColor", "blue");

			mockMvc.perform(patch("/onboard")
				.header("Authorization", "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CommonErrorCode.INVALID_PARAMETER"));
		}
	}
}
