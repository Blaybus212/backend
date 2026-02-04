package com.blaybus.backend.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles({"dev", "secret"})
@Transactional
class UserEntityListenerTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void mockUser_onboarding_should_be_prevented() {
		// Given: Mock 사용자 생성
		User mockUser = User.builder()
			.username("mock-test-user")
			.password(passwordEncoder.encode("password"))
			.isMockUser(true)
			.build();
		userRepository.save(mockUser);

		// When: 온보딩 완료 시도
		mockUser.setOnboardingCompleted(true);
		userRepository.save(mockUser);

		// Then: @PreUpdate에 의해 false로 되돌려져야 함
		User savedUser = userRepository.findByUsername("mock-test-user").orElseThrow();
		assertThat(savedUser.isOnboardingCompleted()).isFalse();
	}

	@Test
	void normalUser_onboarding_should_work() {
		// Given: 일반 사용자 생성
		User normalUser = User.builder()
			.username("normal-test-user")
			.password(passwordEncoder.encode("password"))
			.isMockUser(false)
			.build();
		userRepository.save(normalUser);

		// When: 온보딩 완료
		normalUser.setOnboardingCompleted(true);
		userRepository.save(normalUser);

		// Then: 정상적으로 true로 저장되어야 함
		User savedUser = userRepository.findByUsername("normal-test-user").orElseThrow();
		assertThat(savedUser.isOnboardingCompleted()).isTrue();
	}
}
