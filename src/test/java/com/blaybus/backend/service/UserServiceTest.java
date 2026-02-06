package com.blaybus.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.blaybus.backend.domain.user.EducationLevel;
import com.blaybus.backend.domain.user.Persona;
import com.blaybus.backend.domain.user.ThemeColor;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.OnboardDto;
import com.blaybus.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepository userRepository;

	@Test
	@DisplayName("온보딩 성공 - 모든 필드가 정상적으로 업데이트됨")
	void onboard_success_all_fields_updated() {
		// given
		String username = "testuser";
		User user = User.builder()
			.username(username)
			.password("encodedPassword")
			.onBoardingCompleted(false)
			.build();

		OnboardDto.OnboardRequest request = new OnboardDto.OnboardRequest(
			"홍길동",
			"기계공학,우주공학",
			EducationLevel.BEGINNER,
			"전기공학",
			Persona.FRIEND,
			ThemeColor.BLUE);

		given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

		// when
		userService.handleOnboard(username, request);

		// then - 모든 필드가 정상적으로 채워졌는지 검증
		assertThat(user.getName()).isEqualTo("홍길동");
		assertThat(user.getPreferCategory()).isEqualTo("기계공학,우주공학");
		assertThat(user.getEducationLevel()).isEqualTo(EducationLevel.BEGINNER);
		assertThat(user.getSpecializedIn()).isEqualTo("전기공학");
		assertThat(user.getPersona()).isEqualTo(Persona.FRIEND);
		assertThat(user.getThemeColor()).isEqualTo(ThemeColor.BLUE);
		assertThat(user.isOnBoardingCompleted()).isTrue();
	}

	@Test
	@DisplayName("온보딩 성공 - 각 EducationLevel 값이 올바르게 저장됨")
	void onboard_success_education_level_values() {
		// given
		String username = "testuser";
		User user = User.builder()
			.username(username)
			.password("encodedPassword")
			.build();

		OnboardDto.OnboardRequest request = new OnboardDto.OnboardRequest(
			"홍길동",
			"기계공학",
			EducationLevel.EXPERT,
			"전기공학",
			Persona.PROFESSOR,
			ThemeColor.GREEN);

		given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

		// when
		userService.handleOnboard(username, request);

		// then
		assertThat(user.getEducationLevel()).isEqualTo(EducationLevel.EXPERT);
		assertThat(user.getPersona()).isEqualTo(Persona.PROFESSOR);
		assertThat(user.getThemeColor()).isEqualTo(ThemeColor.GREEN);
	}

	@Test
	@DisplayName("온보딩 성공 - onBoardingCompleted가 true로 변경됨")
	void onboard_success_onboarding_completed_set_to_true() {
		// given
		String username = "testuser";
		User user = User.builder()
			.username(username)
			.password("encodedPassword")
			.onBoardingCompleted(false)
			.build();

		assertThat(user.isOnBoardingCompleted()).isFalse();

		OnboardDto.OnboardRequest request = new OnboardDto.OnboardRequest(
			"홍길동",
			"기계공학",
			EducationLevel.INTERMEDIATE,
			"전기공학",
			Persona.ASSISTANT,
			ThemeColor.PINK);

		given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

		// when
		userService.handleOnboard(username, request);

		// then
		assertThat(user.isOnBoardingCompleted()).isTrue();
	}
}
