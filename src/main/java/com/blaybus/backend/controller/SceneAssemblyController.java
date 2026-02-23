package com.blaybus.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blaybus.backend.dto.scene.DisassemblyLevelDto;
import com.blaybus.backend.dto.scene.SceneAssemblyDto;
import com.blaybus.backend.dto.scene.SceneSyncDto;
import com.blaybus.backend.security.CustomUserDetails;
import com.blaybus.backend.service.SceneAssemblyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/scenes")
@RequiredArgsConstructor
public class SceneAssemblyController {

	private final SceneAssemblyService sceneAssemblyService;

	@PostMapping("/assembly")
	public ResponseEntity<Void> saveAssembly(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody SceneAssemblyDto dto) {

		sceneAssemblyService.saveAssembly(userDetails.getUserId(), dto);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{sceneId}/sync")
	public ResponseEntity<Void> syncScene(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("sceneId") Long sceneId,
			@RequestBody SceneSyncDto dto) {

		sceneAssemblyService.syncSceneState(userDetails.getUserId(), sceneId, dto);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{sceneId}/viewer")
	public ResponseEntity<byte[]> getViewer(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("sceneId") Long sceneId,
			@RequestParam(name = "target", required = false, defaultValue = "both") String target) {

		byte[] zipBytes = sceneAssemblyService.getViewerZip(userDetails.getUserId(), sceneId, target);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"viewer_assets.zip\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(zipBytes);
	}

	@GetMapping("/{sceneId}/disassembly-level")
	public ResponseEntity<DisassemblyLevelDto> getDisassemblyLevel(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("sceneId") Long sceneId) {

		DisassemblyLevelDto response = sceneAssemblyService.getDisassemblyLevel(userDetails.getUserId(), sceneId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{sceneId}/disassembly-level")
	public ResponseEntity<Void> updateDisassemblyLevel(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("sceneId") Long sceneId,
			@RequestBody DisassemblyLevelDto dto) {

		sceneAssemblyService.updateDisassemblyLevel(userDetails.getUserId(), sceneId, dto.getDisassemblyLevel());
		return ResponseEntity.ok().build();
	}

	// TODO: 컴포넌트 클릭 상태 기록 API 추가 필요
	// - Endpoint: POST /scenes/{sceneId}/components/{nodeName}/click
	// - 기능: 사용자가 특정 컴포넌트를 클릭했을 때 Alignment 엔티티의 클릭 상태(isClicked)를 true로 업데이트
}
