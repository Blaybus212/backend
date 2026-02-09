package com.blaybus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.QuizDto;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.QuizGradingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/scenes/{sceneId}/quiz")
@RequiredArgsConstructor
public class QuizController {

	private final QuizGradingService gradingService;
	private final UserRepository userRepository;

	@PostMapping("/{quizId}/grade")
	public ResponseEntity<QuizDto.GradeResponse> grade(
		@AuthenticationPrincipal
		CustomUserDetails userDetails,
		@PathVariable("sceneId")
		Long sceneId,
		@PathVariable("quizId")
		Long quizId,
		@Valid @RequestBody
		QuizDto.GradeRequest request) {
		User user = userRepository.findByUsername(userDetails.getUsername())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

		QuizDto.GradeResponse response = gradingService.grade(quizId, request.answer(), user);
		return ResponseEntity.ok(response);
	}
}
