package com.blaybus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.dto.scene.UserSceneNoteRequest;
import com.blaybus.backend.dto.scene.UserSceneNoteResponse;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.UserSceneService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/scenes")
@RequiredArgsConstructor
public class UserSceneController {

	private final UserSceneService userSceneService;

	@GetMapping("/{sceneId}/note")
	public ResponseEntity<UserSceneNoteResponse> getNote(
		@AuthenticationPrincipal
		CustomUserDetails userDetails,
		@PathVariable(name = "sceneId")
		Long sceneId) {

		UserSceneNoteResponse response = userSceneService.getNote(userDetails.getUserId(), sceneId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{sceneId}/note")
	public ResponseEntity<UserSceneNoteResponse> updateNote(
		@AuthenticationPrincipal
		CustomUserDetails userDetails,
		@PathVariable(name = "sceneId")
		Long sceneId,
		@RequestBody @Validated
		UserSceneNoteRequest request) {

		UserSceneNoteResponse response = userSceneService.updateNote(
			userDetails.getUserId(),
			sceneId,
			request.getContent());
		return ResponseEntity.ok(response);
	}
}
