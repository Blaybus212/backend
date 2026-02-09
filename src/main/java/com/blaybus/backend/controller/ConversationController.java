package com.blaybus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.ConversationDto.ConversationResponse;
import com.blaybus.backend.dto.ConversationDto.SendMessageRequest;
import com.blaybus.backend.dto.ConversationDto.SendMessageResponse;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.ConversationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/scenes/{sceneId}")
@RequiredArgsConstructor
public class ConversationController {

	private final ConversationService conversationService;
	private final UserRepository userRepository;

	@GetMapping("/conversation")
	public ResponseEntity<ConversationResponse> getConversation(
		@PathVariable
		Long sceneId,
		@RequestParam(defaultValue = "5")
		int limit,
		@RequestParam(required = false)
		Long cursor,
		@AuthenticationPrincipal
		CustomUserDetails userDetails) {
		User user = findUser(userDetails.getUsername());
		ConversationResponse response = conversationService.getConversation(user, sceneId, cursor, limit);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/conversation/messages")
	public ResponseEntity<SendMessageResponse> sendMessage(
		@PathVariable
		Long sceneId,
		@Valid @RequestBody
		SendMessageRequest request,
		@AuthenticationPrincipal
		CustomUserDetails userDetails) {
		User user = findUser(userDetails.getUsername());
		SendMessageResponse response = conversationService.sendMessage(user, sceneId, request);
		return ResponseEntity.ok(response);
	}

	private User findUser(String username) {
		return userRepository.findByUsername(username)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
	}
}
