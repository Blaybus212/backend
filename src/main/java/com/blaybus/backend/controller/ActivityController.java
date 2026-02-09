package com.blaybus.backend.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.dto.ActivityResponse;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.ActivityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ActivityController {

	private final ActivityService activityService;

	@GetMapping("/my/activity")
	public ResponseEntity<ActivityResponse> getActivity(@AuthenticationPrincipal
	CustomUserDetails userDetails) {
		ActivityResponse response = activityService.getMonthlyActivity(userDetails.getUserId(), LocalDate.now());
		return ResponseEntity.ok(response);
	}
}
