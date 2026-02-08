package com.blaybus.backend.controller;

import com.blaybus.backend.dto.SceneResponse;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.SceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    @GetMapping("/my/recent/scenes")
    public ResponseEntity<SceneResponse> getScenes(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SceneResponse response = sceneService.getLearningScenes(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
