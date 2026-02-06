package com.blaybus.backend.usecase;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.blaybus.backend.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.AuthDto;

import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
			.build();
		invalidUser = User.builder()
			.username(UUID.randomUUID().toString())
			.password(passwordEncoder.encode("password123"))
			.build();
		userRepository.save(user);
	}

	@Test
	void login_Success() throws Exception {
		AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
			.username(user.getUsername())
			.password("password123")
			.build();

		mockMvc.perform(post("/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	void login_Fail() throws Exception {
		AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
			.username(invalidUser.getUsername())
			.password("wrongpassword")
			.build();

		mockMvc.perform(post("/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}
}
