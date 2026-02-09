package com.blaybus.backend.controller;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.dto.SceneListOrder;
import com.blaybus.backend.dto.SceneListResponse;
import com.blaybus.backend.dto.SceneRankResponse;
import com.blaybus.backend.dto.SceneResponse;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.SceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/scenes/ranks")
    public ResponseEntity<SceneRankResponse> getSceneRanks(
            @RequestParam(required = false) SceneCategory category) {
        SceneRankResponse response = sceneService.getSceneRanks(category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/scenes")
    public ResponseEntity<SceneListResponse> getSceneList(
            @RequestParam(required = false) SceneCategory category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int limit,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "alphabetical") SceneListOrder order) { // 가나다순 → alphabetical,
                                                                                 // 인기순 →popularity
        SceneListResponse response = sceneService.getScenes(category, page, limit, query, order);
        return ResponseEntity.ok(response);
    }
}
