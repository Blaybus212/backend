package com.blaybus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.dto.AuthDto;
import com.blaybus.backend.dto.OnboardDto;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.AuthService;
import com.blaybus.backend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserService userService;

	@PostMapping("/login")
	public ResponseEntity<AuthDto.LoginResponse> login(@RequestBody @Valid
	AuthDto.LoginRequest request) {
		AuthDto.LoginResponse response = authService.handleLogin(request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/onboard")
	public ResponseEntity<Void> onboard(
		@AuthenticationPrincipal
		CustomUserDetails userDetails,
		@RequestBody
		OnboardDto.OnboardRequest request) {
		userService.handleOnboard(userDetails.getUsername(), request);
		return ResponseEntity.noContent().build();
	}
}
