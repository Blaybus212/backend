package com.blaybus.backend.domain;

import com.blaybus.backend.domain.user.User;

import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * User 엔티티의 생명주기 이벤트를 처리하는 리스너
 * Mock 사용자의 onboarding 완료를 방지합니다. (매번 다른 평가자 분들이 접근할 때 마다 온보딩 화면을 보여줘야함)
 */
@Slf4j
public class UserEntityListener {

	@PreUpdate
	public void preventMockUserOnboarding(User user) {
		// Mock 사용자가 아니면 즉시 리턴 (성능 최적화)
		if (!user.isMockUser()) {
			return;
		}

		// Mock 사용자가 onboarding을 완료하려 하면 강제로 false로 되돌림
		if (user.isOnBoardingCompleted()) {
			user.setOnBoardingCompleted(false);
			log.warn("Mock user onboarding completion prevented: username={}", user.getUsername());
		}
	}
}
