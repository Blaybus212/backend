package com.blaybus.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.blaybus.backend.domain.user.User;
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
			.name("테스트유저")
			.onBoardingCompleted(false)
			.preferCategory(com.blaybus.backend.domain.scene.SceneCategory.ROBOTICS)
			.build();

		given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
		given(jwtTokenProvider.createToken("testuser")).willReturn("access.token.value");

		// when
		AuthDto.LoginResponse response = authService.handleLogin(request);

		// then
		assertThat(response.accessToken()).isEqualTo("access.token.value");
		assertThat(response.loginUser()).isNotNull();
		assertThat(response.loginUser().username()).isEqualTo("testuser");
		assertThat(response.loginUser().name()).isEqualTo("테스트유저");
		assertThat(response.loginUser().isFinishOnboard()).isFalse();
		assertThat(response.loginUser().preferCategory())
			.isEqualTo(com.blaybus.backend.domain.scene.SceneCategory.ROBOTICS);
	}

	@Test
	@DisplayName("로그인 성공 - 온보딩 완료된 사용자")
	void login_success_onboarding_completed() {
		// given
		AuthDto.LoginRequest request = new AuthDto.LoginRequest("onboardeduser", "password123");
		User user = User.builder()
			.username("onboardeduser")
			.password("encodedPassword")
			.name("완료유저")
			.onBoardingCompleted(true)
			.preferCategory(com.blaybus.backend.domain.scene.SceneCategory.AUTOMOTIVE_ENGINEERING)
			.build();

		given(userRepository.findByUsername("onboardeduser")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
		given(jwtTokenProvider.createToken("onboardeduser")).willReturn("access.token.value");

		// when
		AuthDto.LoginResponse response = authService.handleLogin(request);

		// then
		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.loginUser().isFinishOnboard()).isTrue();
		assertThat(response.loginUser().name()).isEqualTo("완료유저");
		assertThat(response.loginUser().preferCategory())
			.isEqualTo(com.blaybus.backend.domain.scene.SceneCategory.AUTOMOTIVE_ENGINEERING);
	}

	@Test
	@DisplayName("로그인 성공 - 온보딩 미완료 사용자")
	void login_success_name_is_null() {
		// given
		AuthDto.LoginRequest request = new AuthDto.LoginRequest("noname", "password123");
		User user = User.builder()
			.username("noname")
			.password("encodedPassword")
			.name(null)
			.onBoardingCompleted(false)
			.build();

		given(userRepository.findByUsername("noname")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
		given(jwtTokenProvider.createToken("noname")).willReturn("access.token.value");

		// when
		AuthDto.LoginResponse response = authService.handleLogin(request);

		// then
		assertThat(response.loginUser().name()).isNull();
		assertThat(response.loginUser().username()).isEqualTo("noname");
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
