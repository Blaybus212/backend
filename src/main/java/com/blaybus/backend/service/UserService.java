package com.blaybus.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.OnboardDto;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional
	public void handleOnboard(String username, OnboardDto.OnboardRequest request) {
		User user = userRepository.findByUsername(username)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

		user.setName(request.name());
		user.setPreferCategory(request.preferCategory());
		user.setEducationLevel(request.educationLevel());
		user.setSpecializedIn(request.specialized());
		user.setPersona(request.persona());
		user.setThemeColor(request.themeColor());
		user.setOnBoardingCompleted(true);
	}
}
