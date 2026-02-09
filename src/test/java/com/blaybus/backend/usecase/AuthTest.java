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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.blaybus.backend.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.AuthDto;

import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E2E 기능 테스트
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AuthTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private ObjectMapper objectMapper;

	private User user, invalidUser;

	@BeforeEach
	void init() {
		objectMapper = new ObjectMapper();
		user = User.builder()
			.username(UUID.randomUUID().toString())
			.password(passwordEncoder.encode("password123"))
			.name("테스트유저")
			.onBoardingCompleted(false)
			.preferCategory(com.blaybus.backend.domain.scene.SceneCategory.ROBOTICS)
			.build();
		invalidUser = User.builder()
			.username(UUID.randomUUID().toString())
			.password(passwordEncoder.encode("password123"))
			.build();
		userRepository.save(user);
	}

	@Nested
	@DisplayName("로그인 성공 테스트")
	class LoginSuccessTest {

		@Test
		@DisplayName("로그인 성공 - 응답 형식 검증 (loginUser, accessToken)")
		void login_Success_ResponseFormat() throws Exception {
			AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
				.username(user.getUsername())
				.password("password123")
				.build();

			mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				// 최상위 필드 검증
				.andExpect(jsonPath("$.loginUser").exists())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.accessToken").isString())
				// loginUser 내부 필드 검증
				.andExpect(jsonPath("$.loginUser.username").value(user.getUsername()))
				.andExpect(jsonPath("$.loginUser.name").value("테스트유저"))
				.andExpect(jsonPath("$.loginUser.isFinishOnboard").value(false))
				.andExpect(jsonPath("$.loginUser.preferCategory").value("robotics"));
		}

		@Test
		@DisplayName("로그인 성공 - 온보딩 완료 사용자 (isFinishOnboard: true)")
		void login_Success_OnboardingCompleted() throws Exception {
			User completedUser = User.builder()
				.username(UUID.randomUUID().toString())
				.password(passwordEncoder.encode("password123"))
				.name("완료유저")
				.onBoardingCompleted(true)
				.preferCategory(com.blaybus.backend.domain.scene.SceneCategory.MANUFACTURING_ENGINEERING)
				.build();
			userRepository.save(completedUser);

			AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
				.username(completedUser.getUsername())
				.password("password123")
				.build();

			mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.loginUser.isFinishOnboard").value(true))
				.andExpect(jsonPath("$.loginUser.name").value("완료유저"))
				.andExpect(jsonPath("$.loginUser.preferCategory").value("manufacturing_engineering"));
		}

		@Test
		@DisplayName("로그인 성공 - 온보딩 미완료 사용자")
		void login_Success_NameIsNull() throws Exception {
			User noNameUser = User.builder()
				.username(UUID.randomUUID().toString())
				.password(passwordEncoder.encode("password123"))
				.name(null)
				.onBoardingCompleted(false)
				.build();
			userRepository.save(noNameUser);

			AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
				.username(noNameUser.getUsername())
				.password("password123")
				.build();

			mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.loginUser.username").value(noNameUser.getUsername()))
				.andExpect(jsonPath("$.loginUser.name").isEmpty());
		}
	}

	@Nested
	@DisplayName("로그인 실패 테스트")
	class LoginFailTest {

		@Test
		@DisplayName("로그인 실패 - 존재하지 않는 사용자")
		void login_Fail_UserNotFound() throws Exception {
			AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
				.username(invalidUser.getUsername())
				.password("password123")
				.build();

			mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("로그인 실패 - 비밀번호 불일치")
		void login_Fail_WrongPassword() throws Exception {
			AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
				.username(user.getUsername())
				.password("wrongpassword")
				.build();

			mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("로그인 실패 - username 누락")
		void login_Fail_MissingUsername() throws Exception {
			var request = Map.of("password", "password123");

			mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("로그인 실패 - password 누락")
		void login_Fail_MissingPassword() throws Exception {
			var request = Map.of("username", "testuser");

			mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("로그인 실패 - 비밀번호 6자 미만")
		void login_Fail_PasswordTooShort() throws Exception {
			var request = Map.of("username", "testuser", "password", "12345");

			mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		}
	}
}
