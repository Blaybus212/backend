package com.blaybus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalController {

	@GetMapping("/health-check")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("OK");
	}
}
