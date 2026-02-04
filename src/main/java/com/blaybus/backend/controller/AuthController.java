package com.blaybus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.dto.AuthDto;
import com.blaybus.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<AuthDto.LoginResponse> login(@RequestBody @Valid
	AuthDto.LoginRequest request) {
		AuthDto.LoginResponse response = authService.handleLogin(request);
		return ResponseEntity.ok(response);
	}
}
