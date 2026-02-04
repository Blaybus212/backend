package com.blaybus.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.blaybus.backend.domain.User;
import com.blaybus.backend.dto.AuthDto;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.security.JwtTokenProvider;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("로그인 성공")
	void login_success() {
		// given
		AuthDto.LoginRequest request = new AuthDto.LoginRequest("testuser", "password123");
		User user = User.builder()
			.username("testuser")
			.password("encodedPassword")
			.build();

		given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
		given(jwtTokenProvider.createToken("testuser")).willReturn("access.token.value");

		// when
		AuthDto.LoginResponse response = authService.handleLogin(request);

		// then
		assertThat(response.accessToken()).isEqualTo("access.token.value");
	}

	@Test
	@DisplayName("로그인 실패 - 존재하지 않는 사용자")
	void login_fail_user_not_found() {
		// given
		AuthDto.LoginRequest request = new AuthDto.LoginRequest("unknown", "password123");

		given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authService.handleLogin(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("아이디 또는 비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("로그인 실패 - 비밀번호 불일치")
	void login_fail_wrong_password() {
		// given
		AuthDto.LoginRequest request = new AuthDto.LoginRequest("testuser", "wrongpassword");
		User user = User.builder()
			.username("testuser")
			.password("encodedPassword")
			.build();

		given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("wrongpassword", "encodedPassword")).willReturn(false);

		// when & then
		assertThatThrownBy(() -> authService.handleLogin(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("아이디 또는 비밀번호가 일치하지 않습니다.");
	}
}
