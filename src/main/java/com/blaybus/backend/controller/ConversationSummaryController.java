package com.blaybus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.ConversationDto.ConversationSummaryResponse;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.ConversationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationSummaryController {

	private final ConversationService conversationService;
	private final UserRepository userRepository;

	@GetMapping("/summary")
	public ResponseEntity<ConversationSummaryResponse> summarizeAllConversations(
		@AuthenticationPrincipal
		CustomUserDetails userDetails) {
		User user = userRepository.findByUsername(userDetails.getUsername())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
		ConversationSummaryResponse response = conversationService.summarizeAllConversations(user);
		return ResponseEntity.ok(response);
	}
}
